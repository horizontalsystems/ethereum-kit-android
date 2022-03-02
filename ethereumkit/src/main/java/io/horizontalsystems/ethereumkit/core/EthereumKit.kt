package io.horizontalsystems.ethereumkit.core

import android.app.Application
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.core.*
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.decorations.ContractCallDecorator
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.network.*
import io.horizontalsystems.ethereumkit.transactionsyncers.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.Security
import java.util.*
import java.util.logging.Logger

class EthereumKit(
        private val blockchain: IBlockchain,
        private val transactionManager: TransactionManager,
        private val transactionSyncManager: TransactionSyncManager,
        private val internalTransactionSyncer: TransactionInternalTransactionSyncer,
        private val connectionManager: ConnectionManager,
        private val address: Address,
        val chain: Chain,
        val walletId: String,
        val transactionProvider: ITransactionProvider,
        private val decorationManager: DecorationManager,
        private val state: EthereumKitState = EthereumKitState()
) : IBlockchainListener {

    private val logger = Logger.getLogger("EthereumKit")
    private val disposables = CompositeDisposable()

    private val lastBlockHeightSubject = PublishSubject.create<Long>()
    private val syncStateSubject = PublishSubject.create<SyncState>()
    private val accountStateSubject = PublishSubject.create<AccountState>()

    val defaultGasLimit: Long = 21_000
    private val maxGasLimit: Long = 2_000_000
    private val defaultMinAmount: BigInteger = BigInteger.ONE

    private var started = false

    init {
        state.lastBlockHeight = blockchain.lastBlockHeight
        state.accountState = blockchain.accountState

        transactionManager.allTransactionsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    blockchain.syncAccountState()
                    internalTransactionSyncer.sync(transactions)
                }.let {
                    disposables.add(it)
                }
    }

    val lastBlockHeight: Long?
        get() = state.lastBlockHeight

    val accountState: AccountState?
        get() = state.accountState

    val syncState: SyncState
        get() = blockchain.syncState

    val transactionsSyncState: SyncState
        get() = transactionSyncManager.syncState

    val receiveAddress: Address
        get() = address

    val lastBlockHeightFlowable: Flowable<Long>
        get() = lastBlockHeightSubject.toFlowable(BackpressureStrategy.BUFFER)

    val syncStateFlowable: Flowable<SyncState>
        get() = syncStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsSyncStateFlowable: Flowable<SyncState>
        get() = transactionSyncManager.syncStateAsync

    val accountStateFlowable: Flowable<AccountState>
        get() = accountStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val allTransactionsFlowable: Flowable<List<FullTransaction>>
        get() = transactionManager.allTransactionsAsync

    fun start() {
        if (started)
            return
        started = true

        blockchain.start()
    }

    fun stop() {
        started = false
        blockchain.stop()
        state.clear()
        transactionSyncManager.stop()
    }

    fun refresh() {
        blockchain.refresh()
    }

    fun onEnterForeground() {
        connectionManager.onEnterForeground()
    }

    fun onEnterBackground() {
        connectionManager.onEnterBackground()
    }

    fun getTransactionsFlowable(tags: List<List<String>>): Flowable<List<FullTransaction>> {
        return transactionManager.getTransactionsFlowable(tags)
    }

    fun getTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return transactionManager.getTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingTransactions(tags: List<List<String>>): List<FullTransaction> {
        return transactionManager.getPendingTransactions(tags)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionManager.getFullTransactions(hashes)
    }

    fun estimateGas(to: Address?, value: BigInteger, gasPrice: GasPrice): Single<Long> {
        // without address - provide default gas limit
        if (to == null) {
            return Single.just(defaultGasLimit)
        }

        // if amount is 0 - set default minimum amount
        val resolvedAmount = if (value == BigInteger.ZERO) defaultMinAmount else value

        return blockchain.estimateGas(to, resolvedAmount, maxGasLimit, gasPrice, null)
    }

    fun estimateGas(to: Address?, value: BigInteger?, gasPrice: GasPrice, data: ByteArray?): Single<Long> {
        return blockchain.estimateGas(to, value, maxGasLimit, gasPrice, data)
    }

    fun estimateGas(transactionData: TransactionData, gasPrice: GasPrice): Single<Long> {
        return estimateGas(transactionData.to, transactionData.value, gasPrice, transactionData.input)
    }

    fun rawTransaction(
        transactionData: TransactionData,
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long? = null
    ): Single<RawTransaction> {
        return rawTransaction(
            address = transactionData.to,
            value = transactionData.value,
            transactionInput = transactionData.input,
            gasPrice = gasPrice,
            gasLimit = gasLimit,
            nonce = nonce
        )
    }

    fun rawTransaction(
        address: Address,
        value: BigInteger,
        transactionInput: ByteArray = byteArrayOf(),
        gasPrice: GasPrice,
        gasLimit: Long,
        nonce: Long? = null
    ): Single<RawTransaction> {
        val nonceSingle = nonce?.let { Single.just(it) } ?: blockchain.getNonce()

        return nonceSingle.flatMap { nonce ->
            Single.just(RawTransaction(gasPrice, gasLimit, address, value, nonce, transactionInput))
        }
    }

    fun send(rawTransaction: RawTransaction, signature: Signature): Single<FullTransaction> {
        logger.info("send rawTransaction: $rawTransaction")

        return blockchain.send(rawTransaction, signature)
                    .doOnSuccess { transaction ->
                        transactionManager.handle(transaction)
                    }.map {
                        FullTransaction(it)
                    }
    }

    fun decorate(transactionData: TransactionData): ContractMethodDecoration? {
        return decorationManager.decorateTransaction(transactionData)
    }

    fun transferTransactionData(address: Address, value: BigInteger): TransactionData {
        return transactionManager.etherTransferTransactionData(address = address, value = value)
    }

    fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>> {
        return blockchain.getLogs(address, topics, fromBlock, toBlock, pullTimestamps)
    }

    fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        return blockchain.getStorageAt(contractAddress, position, defaultBlockParameter)
    }

    fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest): Single<ByteArray> {
        return blockchain.call(contractAddress, data, defaultBlockParameter)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: $address")
        return lines.joinToString { "\n" }
    }

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Last Block Height"] = state.lastBlockHeight ?: "N/A"
        statusInfo["Sync State"] = blockchain.syncState.toString()
        statusInfo["Blockchain source"] = blockchain.source
        statusInfo["Transactions source"] = "Infura, Etherscan" //TODO

        return statusInfo
    }

    //
    //IBlockchainListener
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        if (state.lastBlockHeight == lastBlockHeight)
            return

        state.lastBlockHeight = lastBlockHeight
        lastBlockHeightSubject.onNext(lastBlockHeight)
    }

    override fun onUpdateSyncState(syncState: SyncState) {
        syncStateSubject.onNext(syncState)
    }

    override fun onUpdateAccountState(accountState: AccountState) {
        if (state.accountState == accountState) return

        state.accountState = accountState
        accountStateSubject.onNext(accountState)
    }

    fun addTransactionSyncer(transactionSyncer: ITransactionSyncer) {
        transactionSyncManager.add(transactionSyncer)
    }

    fun addDecorator(decorator: IDecorator) {
        decorationManager.addDecorator(decorator)
    }

    fun addTransactionWatcher(transactionWatcher: ITransactionWatcher) {
        internalTransactionSyncer.add(transactionWatcher)
    }

    internal fun <T> rpcSingle(rpc: JsonRpc<T>): Single<T> {
        return blockchain.rpcSingle(rpc)
    }

    sealed class SyncState {
        class Synced : SyncState()
        class NotSynced(val error: Throwable) : SyncState()
        class Syncing(val progress: Double? = null) : SyncState()

        override fun toString(): String = when (this) {
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""}"
            is NotSynced -> "NotSynced ${error.javaClass.simpleName} - message: ${error.message}"
            else -> this.javaClass.simpleName
        }

        override fun equals(other: Any?): Boolean {
            if (other !is SyncState)
                return false

            if (other.javaClass != this.javaClass)
                return false

            if (other is Syncing && this is Syncing) {
                return other.progress == this.progress
            }

            return true
        }

        override fun hashCode(): Int {
            if (this is Syncing) {
                return Objects.hashCode(this.progress)
            }
            return Objects.hashCode(this.javaClass.name)
        }
    }

    open class SyncError : Exception() {
        class NotStarted : SyncError()
        class NoNetworkConnection : SyncError()
    }

    companion object {

        private val gson = GsonBuilder()
                .setLenient()
                .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
                .registerTypeAdapter(Long::class.java, LongTypeAdapter())
                .registerTypeAdapter(object : TypeToken<Long?>() {}.type, LongTypeAdapter())
                .registerTypeAdapter(Int::class.java, IntTypeAdapter())
                .registerTypeAdapter(ByteArray::class.java, ByteArrayTypeAdapter())
                .registerTypeAdapter(Address::class.java, AddressTypeAdapter())
                .registerTypeHierarchyAdapter(DefaultBlockParameter::class.java, DefaultBlockParameterTypeAdapter())
                .registerTypeAdapter(object : TypeToken<Optional<RpcTransaction>>() {}.type, OptionalTypeAdapter<RpcTransaction>(RpcTransaction::class.java))
                .registerTypeAdapter(object : TypeToken<Optional<RpcTransactionReceipt>>() {}.type, OptionalTypeAdapter<RpcTransactionReceipt>(RpcTransactionReceipt::class.java))
                .registerTypeAdapter(object : TypeToken<Optional<RpcBlock>>() {}.type, OptionalTypeAdapter<RpcBlock>(RpcBlock::class.java))
                .create()

        fun init() {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(InternalBouncyCastleProvider.getInstance())
        }

        fun getInstance(
                application: Application,
                words: List<String>,
                passphrase: String = "",
                chain: Chain,
                rpcSource: RpcSource,
                transactionSource: TransactionSource,
                walletId: String
        ): EthereumKit {
            val seed = Mnemonic().toSeed(words, passphrase)
            val privateKey = privateKey(seed, chain)
            val address = ethereumAddress(privateKey)
            return getInstance(application, address, chain, rpcSource, transactionSource, walletId)
        }

        fun getInstance(
                application: Application,
                address: Address,
                chain: Chain,
                rpcSource: RpcSource,
                transactionSource: TransactionSource,
                walletId: String
        ): EthereumKit {

            val connectionManager = ConnectionManager(application)

            val syncer: IRpcSyncer = when (rpcSource) {
                is RpcSource.WebSocket -> {
                    val rpcWebSocket = NodeWebSocket(rpcSource.url, gson, rpcSource.auth)
                    val webSocketRpcSyncer = WebSocketRpcSyncer(rpcWebSocket, gson)

                    rpcWebSocket.listener = webSocketRpcSyncer

                    webSocketRpcSyncer
                }
                is RpcSource.Http -> {
                    val apiProvider = NodeApiProvider(rpcSource.urls, chain.blockTime, gson, rpcSource.auth)
                    ApiRpcSyncer(apiProvider, connectionManager)
                }
            }

            val transactionBuilder = TransactionBuilder(address, chain.id)
            val transactionProvider = transactionProvider(transactionSource, address)

            val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(application, walletId, chain)
            val storage = ApiStorage(apiDatabase)

            val blockchain = RpcBlockchain.instance(address, storage, syncer, transactionBuilder)

            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(application, walletId, chain)
            val transactionStorage = TransactionStorage(transactionDatabase)
            val notSyncedTransactionPool = NotSyncedTransactionPool(transactionStorage)

            val ethereumTransactionsProvider = EthereumTransactionSyncer(transactionProvider)
            val userInternalTransactionsProvider = UserInternalTransactionSyncer(transactionProvider, transactionStorage)
            val transactionInternalTransactionSyncer = TransactionInternalTransactionSyncer(transactionProvider, transactionStorage)
            val outgoingPendingTransactionSyncer = PendingTransactionSyncer(blockchain, transactionStorage)

            val transactionSyncer = TransactionSyncer(blockchain, transactionStorage)

            val notSyncedTransactionManager = NotSyncedTransactionManager(notSyncedTransactionPool, transactionStorage)

            val transactionSyncManager = TransactionSyncManager(notSyncedTransactionManager)
            transactionSyncer.listener = transactionSyncManager
            userInternalTransactionsProvider.listener = transactionSyncManager
            transactionInternalTransactionSyncer.listener = transactionSyncManager
            outgoingPendingTransactionSyncer.listener = transactionSyncManager

            transactionSyncManager.add(userInternalTransactionsProvider)
            transactionSyncManager.add(transactionInternalTransactionSyncer)
            transactionSyncManager.add(ethereumTransactionsProvider)
            transactionSyncManager.add(transactionSyncer)
            transactionSyncManager.add(outgoingPendingTransactionSyncer)

            val decorationManager = DecorationManager(address)
            val tagGenerator = TagGenerator(address)
            val transactionManager = TransactionManager(address, transactionSyncManager, transactionStorage, decorationManager, tagGenerator)

            val ethereumKit = EthereumKit(
                    blockchain,
                    transactionManager,
                    transactionSyncManager,
                    transactionInternalTransactionSyncer,
                    connectionManager,
                    address,
                    chain,
                    walletId,
                    transactionProvider,
                    decorationManager
            )

            blockchain.listener = ethereumKit
            transactionSyncManager.set(ethereumKit)

            decorationManager.addDecorator(ContractCallDecorator(address))

            return ethereumKit
        }

        fun privateKey(seed: ByteArray, chain: Chain): BigInteger {
            val hdWallet = HDWallet(seed, chain.coinType)
            return hdWallet.privateKey(0, 0, true).privKey
        }

        fun clear(context: Context, chain: Chain, walletId: String) {
            EthereumDatabaseManager.clear(context, chain, walletId)
        }

        private fun transactionProvider(transactionSource: TransactionSource, address: Address): ITransactionProvider {
            when (transactionSource.type) {
                is TransactionSource.SourceType.Etherscan -> {
                    val service = EtherscanService(transactionSource.type.apiBaseUrl, transactionSource.type.apiKey)
                    return EtherscanTransactionProvider(service, address)
                }
            }
        }

        private fun ethereumAddress(privateKey: BigInteger): Address {
            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            return Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray())
        }

    }

}
