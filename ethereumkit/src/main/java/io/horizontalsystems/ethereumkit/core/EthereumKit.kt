package io.horizontalsystems.ethereumkit.core

import android.app.Application
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.core.ApiRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.IRpcSyncer
import io.horizontalsystems.ethereumkit.api.core.NodeWebSocket
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchain
import io.horizontalsystems.ethereumkit.api.core.WebSocketRpcSyncer
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.ethereumkit.core.storage.Eip20Storage
import io.horizontalsystems.ethereumkit.core.storage.TransactionStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.decorations.EthereumDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.horizontalsystems.ethereumkit.models.TransactionSource
import io.horizontalsystems.ethereumkit.network.AddressTypeAdapter
import io.horizontalsystems.ethereumkit.network.BigIntegerTypeAdapter
import io.horizontalsystems.ethereumkit.network.ByteArrayTypeAdapter
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.horizontalsystems.ethereumkit.network.DefaultBlockParameterTypeAdapter
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.ethereumkit.network.IntTypeAdapter
import io.horizontalsystems.ethereumkit.network.LongTypeAdapter
import io.horizontalsystems.ethereumkit.network.OptionalTypeAdapter
import io.horizontalsystems.ethereumkit.transactionsyncers.EthereumTransactionSyncer
import io.horizontalsystems.ethereumkit.transactionsyncers.InternalTransactionSyncer
import io.horizontalsystems.ethereumkit.transactionsyncers.TransactionSyncManager
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
import java.util.Objects
import java.util.Optional
import java.util.logging.Logger

class EthereumKit(
    private val blockchain: IBlockchain,
    private val nonceProvider: NonceProvider,
    val transactionManager: TransactionManager,
    private val transactionSyncManager: TransactionSyncManager,
    private val connectionManager: ConnectionManager,
    private val address: Address,
    val chain: Chain,
    val walletId: String,
    val transactionProvider: ITransactionProvider,
    val eip20Storage: IEip20Storage,
    private val decorationManager: DecorationManager,
    private val state: EthereumKitState = EthereumKitState()
) : IBlockchainListener {

    private val logger = Logger.getLogger("EthereumKit")
    private val disposables = CompositeDisposable()

    private val lastBlockHeightSubject = PublishSubject.create<Long>()
    private val syncStateSubject = PublishSubject.create<SyncState>()
    private val accountStateSubject = PublishSubject.create<AccountState>()

    val defaultGasLimit: Long = 21_000
    private val defaultMinAmount: BigInteger = BigInteger.ONE

    private var started = false

    init {
        state.lastBlockHeight = blockchain.lastBlockHeight
        state.accountState = blockchain.accountState

        transactionManager.fullTransactionsAsync
            .subscribeOn(Schedulers.io())
            .subscribe {
                blockchain.syncAccountState()
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

    val allTransactionsFlowable: Flowable<Pair<List<FullTransaction>, Boolean>>
        get() = transactionManager.fullTransactionsAsync

    fun start() {
        if (started)
            return
        started = true

        blockchain.start()
        transactionSyncManager.sync()
    }

    fun stop() {
        started = false
        blockchain.stop()
        state.clear()
        connectionManager.stop()
    }

    fun refresh() {
        blockchain.refresh()
        transactionSyncManager.sync()
    }

    fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long> {
        return nonceProvider.getNonce(defaultBlockParameter)
    }

    fun getFullTransactionsFlowable(tags: List<List<String>>): Flowable<List<FullTransaction>> {
        return transactionManager.getFullTransactionsFlowable(tags)
    }

    fun getFullTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return transactionManager.getFullTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingFullTransactions(tags: List<List<String>>): List<FullTransaction> {
        return transactionManager.getPendingFullTransactions(tags)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionManager.getFullTransactions(hashes)
    }

    fun getFullTransactionSingle(hash: ByteArray): Single<FullTransaction> {
        return transactionManager.getFullTransactionSingle(hash)
    }

    fun getFullTransactionsAfterSingle(hash: ByteArray?): Single<List<FullTransaction>> {
        return transactionManager.getFullTransactionsAfterSingle(hash)
    }

    fun estimateGas(to: Address?, value: BigInteger, gasPrice: GasPrice): Single<Long> {
        // without address - provide default gas limit
        if (to == null) {
            return Single.just(defaultGasLimit)
        }

        // if amount is 0 - set default minimum amount
        val resolvedAmount = if (value == BigInteger.ZERO) defaultMinAmount else value

        return blockchain.estimateGas(to, resolvedAmount, chain.gasLimit, gasPrice, null)
    }

    fun estimateGas(to: Address?, value: BigInteger?, gasPrice: GasPrice?, data: ByteArray?): Single<Long> {
        return blockchain.estimateGas(to, value, chain.gasLimit, gasPrice, data)
    }

    fun estimateGas(transactionData: TransactionData, gasPrice: GasPrice? = null): Single<Long> {
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
        val nonceSingle = nonce?.let { Single.just(it) } ?: nonceProvider.getNonce(DefaultBlockParameter.Pending)

        return nonceSingle.flatMap { nonce ->
            Single.just(RawTransaction(gasPrice, gasLimit, address, value, nonce, transactionInput))
        }
    }

    fun send(rawTransaction: RawTransaction, signature: Signature): Single<FullTransaction> {
        logger.info("send rawTransaction: $rawTransaction")

        return blockchain.send(rawTransaction, signature)
            .map { transactionManager.handle(listOf(it)).first() }
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        return decorationManager.decorateTransaction(address, transactionData)
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

    fun call(
        contractAddress: Address,
        data: ByteArray,
        defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
    ): Single<ByteArray> {
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

    fun getTagTokenContractAddresses(): List<String> {
        return transactionManager.getDistinctTokenContractAddresses()
    }

    //
    //IBlockchainListener
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        if (state.lastBlockHeight == lastBlockHeight)
            return

        state.lastBlockHeight = lastBlockHeight
        lastBlockHeightSubject.onNext(lastBlockHeight)
        transactionSyncManager.sync()
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

    fun addNonceProvider(provider: INonceProvider) {
        nonceProvider.addProvider(provider)
    }

    fun addExtraDecorator(decorator: IExtraDecorator) {
        decorationManager.addExtraDecorator(decorator)
    }

    fun addMethodDecorator(decorator: IMethodDecorator) {
        decorationManager.addMethodDecorator(decorator)
    }

    fun addEventDecorator(decorator: IEventDecorator) {
        decorationManager.addEventDecorator(decorator)
    }

    fun addTransactionDecorator(decorator: ITransactionDecorator) {
        decorationManager.addTransactionDecorator(decorator)
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

        val gson = GsonBuilder()
            .setLenient()
            .registerTypeAdapter(BigInteger::class.java, BigIntegerTypeAdapter())
            .registerTypeAdapter(Long::class.java, LongTypeAdapter())
            .registerTypeAdapter(object : TypeToken<Long?>() {}.type, LongTypeAdapter())
            .registerTypeAdapter(Int::class.java, IntTypeAdapter())
            .registerTypeAdapter(ByteArray::class.java, ByteArrayTypeAdapter())
            .registerTypeAdapter(Address::class.java, AddressTypeAdapter())
            .registerTypeHierarchyAdapter(DefaultBlockParameter::class.java, DefaultBlockParameterTypeAdapter())
            .registerTypeAdapter(
                object : TypeToken<Optional<RpcTransaction>>() {}.type,
                OptionalTypeAdapter<RpcTransaction>(RpcTransaction::class.java)
            )
            .registerTypeAdapter(
                object : TypeToken<Optional<RpcTransactionReceipt>>() {}.type,
                OptionalTypeAdapter<RpcTransactionReceipt>(RpcTransactionReceipt::class.java)
            )
            .registerTypeAdapter(object : TypeToken<Optional<RpcBlock>>() {}.type, OptionalTypeAdapter<RpcBlock>(RpcBlock::class.java))
            .create()

        fun call(
            rpcSource: RpcSource,
            contractAddress: Address,
            data: ByteArray,
            defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest
        ): Single<ByteArray> {
            val rpcApiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)
            val rpc = RpcBlockchain.callRpc(contractAddress, data, defaultBlockParameter)
            return rpcApiProvider.single(rpc)
        }

        fun estimateGas(
            rpcSource: RpcSource,
            chain: Chain,
            from: Address,
            to: Address?,
            value: BigInteger?,
            gasPrice: GasPrice,
            data: ByteArray?
        ): Single<Long> {
            return RpcBlockchain.estimateGas(rpcSource, from, to, value, chain.gasLimit, gasPrice, data)
        }

        fun estimateGas(
            rpcSource: RpcSource,
            chain: Chain,
            from: Address,
            transactionData: TransactionData,
            gasPrice: GasPrice
        ): Single<Long> {
            return estimateGas(rpcSource, chain, from, transactionData.to, transactionData.value, gasPrice, transactionData.input)
        }

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
            val privateKey = Signer.privateKey(seed, chain)
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
                    val rpcWebSocket = NodeWebSocket(rpcSource.uri, gson, rpcSource.auth)
                    val webSocketRpcSyncer = WebSocketRpcSyncer(rpcWebSocket, gson)

                    rpcWebSocket.listener = webSocketRpcSyncer

                    webSocketRpcSyncer
                }

                is RpcSource.Http -> {
                    val apiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)
                    ApiRpcSyncer(apiProvider, connectionManager, chain.syncInterval)
                }
            }

            val transactionBuilder = TransactionBuilder(address, chain.id)
            val transactionProvider = transactionProvider(transactionSource, address, chain.id)

            val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(application, walletId, chain)
            val storage = ApiStorage(apiDatabase)

            val blockchain = RpcBlockchain.instance(address, storage, syncer, transactionBuilder)

            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(application, walletId, chain)
            val transactionStorage = TransactionStorage(transactionDatabase)
            val transactionSyncerStateStorage = TransactionSyncerStateStorage(transactionDatabase)

            val erc20Database = EthereumDatabaseManager.getErc20Database(application, walletId, chain)
            val erc20Storage = Eip20Storage(erc20Database)

            val ethereumTransactionSyncer = EthereumTransactionSyncer(transactionProvider, transactionSyncerStateStorage)
            val internalTransactionsSyncer = InternalTransactionSyncer(transactionProvider, transactionStorage)

            val decorationManager = DecorationManager(address, transactionStorage)
            val transactionManager = TransactionManager(address, transactionStorage, decorationManager, blockchain, transactionProvider)
            val transactionSyncManager = TransactionSyncManager(transactionManager)

            transactionSyncManager.add(internalTransactionsSyncer)
            transactionSyncManager.add(ethereumTransactionSyncer)

            val nonceProvider = NonceProvider()
            nonceProvider.addProvider(blockchain)

            val ethereumKit = EthereumKit(
                blockchain,
                nonceProvider,
                transactionManager,
                transactionSyncManager,
                connectionManager,
                address,
                chain,
                walletId,
                transactionProvider,
                erc20Storage,
                decorationManager
            )

            blockchain.listener = ethereumKit

            decorationManager.addTransactionDecorator(EthereumDecorator(address))

            return ethereumKit
        }

        fun clear(context: Context, chain: Chain, walletId: String) {
            EthereumDatabaseManager.clear(context, chain, walletId)
        }

        private fun transactionProvider(transactionSource: TransactionSource, address: Address, chainId: Int): ITransactionProvider {
            when (transactionSource.type) {
                is TransactionSource.SourceType.Etherscan -> {
                    val service = EtherscanService(transactionSource.type.apiBaseUrl, transactionSource.type.apiKeys, chainId)
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
