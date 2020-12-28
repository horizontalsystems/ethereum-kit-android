package io.horizontalsystems.ethereumkit.core

import android.app.Application
import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.*
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.core.refactoring.*
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.network.*
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.*
import java.util.logging.Logger

class EthereumKit(
        val blockchain: IBlockchain, // make public for testing
        private val transactionManager: TransactionManager,
        private val transactionSyncManager: TransactionSyncManager,
        private val transactionBuilder: TransactionBuilder,
        private val transactionSigner: TransactionSigner,
        private val connectionManager: ConnectionManager,
        private val address: Address,
        val networkType: NetworkType,
        val walletId: String,
        val etherscanKey: String,
        private val state: EthereumKitState = EthereumKitState()
) : IBlockchainListener {

    private val logger = Logger.getLogger("EthereumKit")

    private val lastBlockBloomFilterSubject = PublishSubject.create<BloomFilter>()
    private val lastBlockHeightSubject = PublishSubject.create<Long>()
    private val syncStateSubject = PublishSubject.create<SyncState>()
    private val balanceSubject = PublishSubject.create<BigInteger>()
    private val nonceSubject = PublishSubject.create<Long>()

    val defaultGasLimit: Long = 21_000
    private val maxGasLimit: Long = 1_000_000
    private val defaultMinAmount: BigInteger = BigInteger.ONE

    private var started = false

    init {
        state.balance = blockchain.balance
        state.lastBlockHeight = blockchain.lastBlockHeight
    }

    val lastBlockHeight: Long?
        get() = state.lastBlockHeight

    val balance: BigInteger?
        get() = state.balance

    val syncState: SyncState
        get() = blockchain.syncState

    val transactionsSyncState: SyncState
        get() = transactionSyncManager.syncState

    val receiveAddress: Address
        get() = address

    val lastBlockHeightFlowable: Flowable<Long>
        get() = lastBlockHeightSubject.toFlowable(BackpressureStrategy.BUFFER)

    val lastBlockBloomFilterFlowable: Flowable<BloomFilter>
        get() = lastBlockBloomFilterSubject.toFlowable(BackpressureStrategy.BUFFER)

    val syncStateFlowable: Flowable<SyncState>
        get() = syncStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsSyncStateFlowable: Flowable<SyncState>
        get() = transactionSyncManager.syncStateAsync

    val balanceFlowable: Flowable<BigInteger>
        get() = balanceSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsFlowable: Flowable<List<FullTransaction>>
        get() = transactionManager.etherTransactionsAsync

    val nonceFlowable: Flowable<Long>
        get() = nonceSubject.toFlowable(BackpressureStrategy.BUFFER)

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
    }

    fun refresh() {
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

    fun transactionStatus(transactionHash: ByteArray): Single<TransactionStatus> {
        return blockchain.getTransactionReceipt(transactionHash)
                .flatMap { receipt ->
                    when {
                        receipt.isPresent -> {
                            if (receipt.get().status == 1) {
                                Single.just(TransactionStatus.SUCCESS)
                            } else {
                                Single.just(TransactionStatus.FAILED)
                            }
                        }
                        else -> blockchain.getTransaction(transactionHash)
                                .map { transaction ->
                                    if (transaction.isPresent) {
                                        TransactionStatus.PENDING
                                    } else {
                                        TransactionStatus.NOTFOUND
                                    }
                                }
                    }
                }
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

    fun send(to: Address, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long, nonce: Long? = null): Single<Transaction> {
        val nonceSingle = nonce?.let { Single.just(it) } ?: blockchain.getNonce()

        return nonceSingle.flatMap { nonce ->
            val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, to, value, nonce, transactionInput)
            logger.info("send rawTransaction: $rawTransaction")

            blockchain.send(rawTransaction)
                    .doOnSuccess { transaction ->
                        transactionManager.handle(transaction)
                    }.map {
                        it
                    }
        }
    }

    fun signedTransaction(address: Address, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long, nonce: Long): ByteArray {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, address, value, nonce, transactionInput)
        val signature = transactionSigner.signature(rawTransaction)
        return transactionBuilder.encode(rawTransaction, signature)
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
        statusInfo["Sync State"] = blockchain.syncState
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

    override fun onUpdateBalance(balance: BigInteger) {
        if (state.balance == balance) return

        state.balance = balance
        balanceSubject.onNext(balance)
    }

    override fun onUpdateLogsBloomFilter(bloomFilter: BloomFilter) {
        lastBlockBloomFilterSubject.onNext(bloomFilter)
    }

    override fun onUpdateNonce(nonce: Long) {
        if (state.nonce == nonce) return

        state.nonce = nonce
        nonceSubject.onNext(nonce)
    }

    sealed class SyncState {
        class Synced : SyncState()
        class NotSynced(val error: Throwable) : SyncState()
        class Syncing(val progress: Double? = null) : SyncState()

        override fun toString(): String = when (this) {
            is Syncing -> "Syncing ${progress?.let { "${it * 100}" } ?: ""}"
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
        fun getInstance(
                application: Application,
                privateKey: BigInteger,
                syncMode: SyncMode,
                networkType: NetworkType,
                syncSource: SyncSource,
                etherscanKey: String,
                walletId: String
        ): EthereumKit {
            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            val address = Address(CryptoUtils.sha3(publicKey).takeLast(20).toByteArray())

            val network = networkType.getNetwork()
            val transactionSigner = TransactionSigner(network, privateKey)
            val transactionBuilder = TransactionBuilder(address)
            val connectionManager = ConnectionManager(application)

            val infuraDomain = when (networkType) {
                NetworkType.MainNet -> "mainnet.infura.io"
                NetworkType.Ropsten -> "ropsten.infura.io"
                NetworkType.Kovan -> "kovan.infura.io"
                NetworkType.Rinkeby -> "rinkeby.infura.io"
            }
            val gson = GsonBuilder()
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

            val syncer: IRpcSyncer = when (syncSource) {
                is SyncSource.Infura -> {
                    ApiRpcSyncer(address, InfuraRpcApiProvider(infuraDomain, syncSource.id, syncSource.secret, gson), connectionManager)
                }
                is SyncSource.InfuraWebSocket -> {
                    val rpcWebSocket: IRpcWebSocket = InfuraRpcWebSocket(infuraDomain, syncSource.id, syncSource.secret, application, gson)
                    val webSocketRpcSyncer = WebSocketRpcSyncer(address, rpcWebSocket, gson)

                    rpcWebSocket.listener = webSocketRpcSyncer

                    webSocketRpcSyncer
                }
                is SyncSource.Incubed -> {
                    ApiRpcSyncer(address, IncubedRpcApiProvider(networkType, gson), connectionManager)
                }
            }

            val blockchain: IBlockchain = when (syncMode) {
                is SyncMode.ApiSyncMode -> {
                    val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(application, walletId, networkType)
                    val storage = ApiStorage(apiDatabase)

                    RpcBlockchain.instance(address, storage, syncer, transactionSigner, transactionBuilder)
                }
                is SyncMode.SpvSyncMode -> {
                    throw Exception("SPV Sync Mode is not supported!")
//                    val spvDatabase = EthereumDatabaseManager.getEthereumSpvDatabase(context, walletId, networkType)
//                    val nodeKey = CryptoUtils.ecKeyFromPrivate(syncMode.nodePrivateKey)
//                    val storage = SpvStorage(spvDatabase)
//
//                    SpvBlockchain.getInstance(storage, transactionSigner, transactionBuilder, rpcApiProvider, network, address, nodeKey)
                }
                is SyncMode.GethSyncMode -> {
                    throw Exception("Geth Sync Mode is not supported!")
//                    val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(context, walletId, networkType)
//                    val storage = ApiStorage(apiDatabase)
//                    val nodeDirectory = context.filesDir.path + "/gethNode"
//
//                    blockchain = GethBlockchain.getInstance(nodeDirectory, network, storage, transactionSigner, transactionBuilder, address)
                }
            }

            val etherscanService = EtherscanService(networkType, etherscanKey)
            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(application, walletId, networkType)
            val storage = Storage(transactionDatabase)
            val notSyncedTransactionPool = NotSyncedTransactionPool(storage)

            val etherscanTransactionsProvider = EtherscanTransactionsProvider(etherscanService, address)
            val ethereumTransactionsProvider = EthereumTransactionSyncer(etherscanTransactionsProvider, notSyncedTransactionPool, storage)
            val internalTransactionsProvider = InternalTransactionSyncer(etherscanTransactionsProvider, notSyncedTransactionPool, storage)
            val outgoingPendingTransactionSyncer = OutgoingPendingTransactionSyncer(blockchain, storage)

            val transactionSyncer = TransactionSyncer(notSyncedTransactionPool, blockchain, storage)

            val transactionSyncManager = TransactionSyncManager()
            transactionSyncer.listener = transactionSyncManager
            outgoingPendingTransactionSyncer.listener = transactionSyncManager

            transactionSyncManager.add(transactionSyncer)
            transactionSyncManager.add(ethereumTransactionsProvider)
            transactionSyncManager.add(internalTransactionsProvider)
            transactionSyncManager.add(outgoingPendingTransactionSyncer)

            val transactionManager = TransactionManager(address, transactionSyncManager, storage)

            val ethereumKit = EthereumKit(blockchain, transactionManager, transactionSyncManager, transactionBuilder, transactionSigner, connectionManager, address, networkType, walletId, etherscanKey)

            blockchain.listener = ethereumKit
            transactionSyncManager.set(ethereumKit)

            return ethereumKit
        }

        fun getInstance(
                application: Application,
                words: List<String>,
                wordsSyncMode: WordsSyncMode,
                networkType: NetworkType,
                syncSource: SyncSource,
                etherscanKey: String,
                walletId: String
        ) = getInstance(application, Mnemonic().toSeed(words), wordsSyncMode, networkType, syncSource, etherscanKey, walletId)

        fun getInstance(
                application: Application,
                seed: ByteArray,
                wordsSyncMode: WordsSyncMode,
                networkType: NetworkType,
                syncSource: SyncSource,
                etherscanKey: String,
                walletId: String
        ): EthereumKit {
            val hdWallet = HDWallet(seed, if (networkType == NetworkType.MainNet) 60 else 1)
            val privateKey = hdWallet.privateKey(0, 0, true).privKey

            val syncMode = when (wordsSyncMode) {
                is WordsSyncMode.SpvSyncMode -> {
                    val nodePrivateKey = hdWallet.privateKey(101, 101, true).privKey
                    SyncMode.SpvSyncMode(nodePrivateKey)
                }
                is WordsSyncMode.ApiSyncMode -> {
                    SyncMode.ApiSyncMode()
                }
            }

            return getInstance(application, privateKey, syncMode, networkType, syncSource, etherscanKey, walletId)
        }

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            EthereumDatabaseManager.clear(context, networkType, walletId)
        }

    }

    sealed class SyncSource {
        class InfuraWebSocket(val id: String, val secret: String?) : SyncSource()
        class Infura(val id: String, val secret: String?) : SyncSource()
        object Incubed : SyncSource()
    }

    sealed class WordsSyncMode {
        class ApiSyncMode : WordsSyncMode()
        class SpvSyncMode : WordsSyncMode()
    }

    sealed class SyncMode {
        class ApiSyncMode : SyncMode()
        class SpvSyncMode(val nodePrivateKey: BigInteger) : SyncMode()
        class GethSyncMode : SyncMode()
    }

    data class InfuraCredentials(val projectId: String, val secretKey: String?)

    enum class NetworkType {
        MainNet,
        Ropsten,
        Kovan,
        Rinkeby;

        fun getNetwork(): INetwork {
            if (this == MainNet) {
                return MainNet()
            }
            return Ropsten()
        }
    }

}
