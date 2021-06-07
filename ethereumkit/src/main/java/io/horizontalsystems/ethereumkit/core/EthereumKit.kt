package io.horizontalsystems.ethereumkit.core

import android.app.Application
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.core.*
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.crypto.*
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
import java.net.URL
import java.security.Security
import java.util.*
import java.util.logging.Logger

class EthereumKit(
        private val blockchain: IBlockchain,
        private val transactionManager: TransactionManager,
        private val transactionSyncManager: TransactionSyncManager,
        private val transactionBuilder: TransactionBuilder,
        private val transactionSigner: TransactionSigner,
        private val connectionManager: ConnectionManager,
        private val address: Address,
        val networkType: NetworkType,
        val walletId: String,
        val etherscanService: EtherscanService,
        private val decorationManager: DecorationManager,
        private val ethSigner: EthSigner,
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

        transactionManager.etherTransactionsAsync
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

    val etherTransactionsFlowable: Flowable<List<FullTransaction>>
        get() = transactionManager.etherTransactionsAsync

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

    fun etherTransactions(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return transactionManager.getEtherTransactionsAsync(fromHash, limit)
    }

    fun getFullTransactions(fromSyncOrder: Long?): List<FullTransaction> {
        return transactionManager.getFullTransactions(fromSyncOrder)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return transactionManager.getFullTransactions(hashes)
    }

    fun estimateGas(to: Address?, value: BigInteger, gasPrice: Long?): Single<Long> {
        // without address - provide default gas limit
        if (to == null) {
            return Single.just(defaultGasLimit)
        }

        // if amount is 0 - set default minimum amount
        val resolvedAmount = if (value == BigInteger.ZERO) defaultMinAmount else value

        return blockchain.estimateGas(to, resolvedAmount, maxGasLimit, gasPrice, null)
    }

    fun estimateGas(to: Address?, value: BigInteger?, gasPrice: Long?, data: ByteArray?): Single<Long> {
        return blockchain.estimateGas(to, value, maxGasLimit, gasPrice, data)
    }

    fun estimateGas(transactionData: TransactionData, gasPrice: Long?): Single<Long> {
        return estimateGas(transactionData.to, transactionData.value, gasPrice, transactionData.input)
    }

    fun send(to: Address, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long, nonce: Long? = null): Single<FullTransaction> {
        val nonceSingle = nonce?.let { Single.just(it) } ?: blockchain.getNonce()

        return nonceSingle.flatMap { nonce ->
            val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, to, value, nonce, transactionInput)
            logger.info("send rawTransaction: $rawTransaction")

            blockchain.send(rawTransaction)
                    .doOnSuccess { transaction ->
                        transactionManager.handle(transaction)
                    }.map {
                        FullTransaction(it)
                    }
        }
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        return decorationManager.decorate(transactionData)
    }

    fun send(transactionData: TransactionData, gasPrice: Long, gasLimit: Long, nonce: Long? = null): Single<FullTransaction> {
        return send(transactionData.to, transactionData.value, transactionData.input, gasPrice, gasLimit, nonce)
    }

    fun signedTransaction(address: Address, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long, nonce: Long): ByteArray {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, address, value, nonce, transactionInput)
        val signature = transactionSigner.signature(rawTransaction)
        return transactionBuilder.encode(rawTransaction, signature)
    }

    fun signByteArray(message: ByteArray): ByteArray {
        return ethSigner.signByteArray(message)
    }

    fun signTypedData(rawJsonMessage: String): ByteArray {
        return ethSigner.signTypedData(rawJsonMessage)
    }

    fun parseTypedData(rawJsonMessage: String): TypedData? {
        return ethSigner.parseTypedData(rawJsonMessage)
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

    fun removeTransactionSyncer(id: String) {
        transactionSyncManager.removeSyncer(id)
    }

    fun addDecorator(decorator: IDecorator) {
        decorationManager.addDecorator(decorator)
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
                networkType: NetworkType,
                syncSource: SyncSource,
                etherscanApiKey: String,
                walletId: String
        ): EthereumKit {
            return getInstance(application, Mnemonic().toSeed(words, passphrase), networkType, syncSource, etherscanApiKey, walletId)
        }

        fun getInstance(
                application: Application,
                seed: ByteArray,
                networkType: NetworkType,
                syncSource: SyncSource,
                etherscanApiKey: String,
                walletId: String
        ): EthereumKit {
            val privateKey = privateKey(seed, networkType)
            val address = ethereumAddress(privateKey)

            val connectionManager = ConnectionManager(application)

            val syncer: IRpcSyncer = when (syncSource) {
                is SyncSource.WebSocket -> {
                    val rpcWebSocket = NodeWebSocket(syncSource.url, gson, syncSource.auth)
                    val webSocketRpcSyncer = WebSocketRpcSyncer(rpcWebSocket, gson)

                    rpcWebSocket.listener = webSocketRpcSyncer

                    webSocketRpcSyncer
                }
                is SyncSource.Http -> {
                    val apiProvider = NodeApiProvider(syncSource.url, syncSource.blockTime, gson, syncSource.auth)
                    ApiRpcSyncer(apiProvider, connectionManager)
                }
            }

            val transactionSigner = TransactionSigner(privateKey, networkType.chainId)
            val transactionBuilder = TransactionBuilder(address)
            val etherscanService = EtherscanService(etherscanApiKey, networkType)

            val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(application, walletId, networkType)
            val storage = ApiStorage(apiDatabase)

            val blockchain = RpcBlockchain.instance(address, storage, syncer, transactionSigner, transactionBuilder)

            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(application, walletId, networkType)
            val transactionStorage = TransactionStorage(transactionDatabase)
            val notSyncedTransactionPool = NotSyncedTransactionPool(transactionStorage)

            val etherscanTransactionsProvider = EtherscanTransactionsProvider(etherscanService, address)
            val ethereumTransactionsProvider = EthereumTransactionSyncer(etherscanTransactionsProvider)
            val internalTransactionsProvider = InternalTransactionSyncer(etherscanTransactionsProvider, transactionStorage)
            val outgoingPendingTransactionSyncer = PendingTransactionSyncer(blockchain, transactionStorage)

            val transactionSyncer = TransactionSyncer(blockchain, transactionStorage)

            val notSyncedTransactionManager = NotSyncedTransactionManager(notSyncedTransactionPool, transactionStorage)

            val transactionSyncManager = TransactionSyncManager(notSyncedTransactionManager)
            transactionSyncer.listener = transactionSyncManager
            outgoingPendingTransactionSyncer.listener = transactionSyncManager

            transactionSyncManager.add(internalTransactionsProvider)
            transactionSyncManager.add(ethereumTransactionsProvider)
            transactionSyncManager.add(transactionSyncer)
            transactionSyncManager.add(outgoingPendingTransactionSyncer)

            val transactionManager = TransactionManager(address, transactionSyncManager, transactionStorage)
            val decorationManager = DecorationManager(address)
            val ethSigner = EthSigner(privateKey, CryptoUtils, EIP712Encoder())

            val ethereumKit = EthereumKit(blockchain, transactionManager, transactionSyncManager, transactionBuilder, transactionSigner, connectionManager, address, networkType, walletId, etherscanService, decorationManager, ethSigner)

            blockchain.listener = ethereumKit
            transactionSyncManager.set(ethereumKit)

            return ethereumKit
        }

        fun address(words: List<String>, passphrase: String = "", networkType: NetworkType): Address {
            return address(Mnemonic().toSeed(words, passphrase), networkType)
        }

        fun address(seed: ByteArray, networkType: NetworkType): Address {
            val privateKey = privateKey(seed, networkType)
            return ethereumAddress(privateKey)
        }

        fun privateKey(words: List<String>, passphrase: String = "", networkType: NetworkType): BigInteger {
            return privateKey(Mnemonic().toSeed(words, passphrase), networkType)
        }

        fun privateKey(seed: ByteArray, networkType: NetworkType): BigInteger {
            val hdWallet = HDWallet(seed, networkType.coinType)
            return hdWallet.privateKey(0, 0, true).privKey
        }

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            EthereumDatabaseManager.clear(context, networkType, walletId)
        }

        fun infuraWebSocketSyncSource(networkType: NetworkType, projectId: String, projectSecret: String?): SyncSource? =
                infuraDomain(networkType)?.let { infuraDomain ->
                    val url = URL("https://$infuraDomain/ws/v3/$projectId")
                    SyncSource.WebSocket(url, projectSecret)
                }

        fun infuraHttpSyncSource(networkType: NetworkType, projectId: String, projectSecret: String?): SyncSource? =
                infuraDomain(networkType)?.let { infuraDomain ->
                    val url = URL("https://$infuraDomain/v3/$projectId")
                    SyncSource.Http(url, networkType.blockTime, projectSecret)
                }

        fun defaultBscWebSocketSyncSource(): SyncSource =
                SyncSource.WebSocket(URL("https://bsc-ws-node.nariox.org:443"), null)


        fun defaultBscHttpSyncSource(): SyncSource =
                SyncSource.Http(URL("https://bsc-dataseed.binance.org"), NetworkType.BscMainNet.blockTime, null)

        private fun infuraDomain(networkType: NetworkType): String? =
                when (networkType) {
                    NetworkType.EthMainNet -> "mainnet.infura.io"
                    NetworkType.EthRopsten -> "ropsten.infura.io"
                    NetworkType.EthKovan -> "kovan.infura.io"
                    NetworkType.EthRinkeby -> "rinkeby.infura.io"
                    NetworkType.EthGoerli -> "goerli.infura.io"
                    NetworkType.BscMainNet -> null
                }

        private fun ethereumAddress(privateKey: BigInteger): Address {
            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            return Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray())
        }

    }

    sealed class SyncSource {
        class WebSocket(val url: URL, val auth: String?) : SyncSource()
        class Http(val url: URL, val blockTime: Long, val auth: String?) : SyncSource()
    }

    enum class NetworkType(
        val chainId: Int,
        val blockTime: Long,
        val coinType: Int,
        val isMainNet: Boolean
    ) {
        EthMainNet(1, 15, 60, true),
        EthRopsten(56, 5, 1, false),
        EthKovan(42, 4, 1, false),
        EthRinkeby(4, 15, 1, false),
        BscMainNet(56, 5, 60, true),
        EthGoerli(5, 15, 1, false);
    }

}
