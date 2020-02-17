package io.horizontalsystems.ethereumkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.api.ApiBlockchain
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionStorage
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.network.MainNet
import io.horizontalsystems.ethereumkit.network.Ropsten
import io.horizontalsystems.ethereumkit.spv.core.SpvBlockchain
import io.horizontalsystems.ethereumkit.spv.core.storage.SpvStorage
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.logging.Logger

class EthereumKit(
        private val blockchain: IBlockchain,
        private val transactionManager: ITransactionManager,
        private val addressValidator: AddressValidator,
        private val transactionBuilder: TransactionBuilder,
        private val address: ByteArray,
        val networkType: NetworkType,
        val walletId: String,
        val etherscanKey: String,
        private val state: EthereumKitState = EthereumKitState())
    : IBlockchainListener, ITransactionManagerListener {

    private val logger = Logger.getLogger("EthereumKit")

    private val lastBlockHeightSubject = PublishSubject.create<Long>()
    private val syncStateSubject = PublishSubject.create<SyncState>()
    private val balanceSubject = PublishSubject.create<BigInteger>()
    private val transactionsSubject = PublishSubject.create<List<TransactionInfo>>()

    val gasLimit: Long = 21_000

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

    val receiveAddress: String
        get() = address.toEIP55Address()

    val receiveAddressRaw: ByteArray
        get() = address

    val lastBlockHeightFlowable: Flowable<Long>
        get() = lastBlockHeightSubject.toFlowable(BackpressureStrategy.BUFFER)

    val syncStateFlowable: Flowable<SyncState>
        get() = syncStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val balanceFlowable: Flowable<BigInteger>
        get() = balanceSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsFlowable: Flowable<List<TransactionInfo>>
        get() = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun start() {
        if (started)
            return
        started = true

        blockchain.start()
        transactionManager.refresh()
    }

    fun stop() {
        started = false
        blockchain.stop()
        state.clear()
    }

    fun refresh() {
        blockchain.refresh()
        transactionManager.refresh()
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPrice: Long): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimit.toBigDecimal())
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return transactionManager.getTransactions(fromHash?.hexStringToByteArray(), limit)
                .map { txs -> txs.map { TransactionInfo(it) } }
    }

    fun transactionStatus(transactionHash: ByteArray): Single<TransactionStatus> {

        return blockchain.transactionReceiptStatus(transactionHash).flatMap { transactionStatus ->

            if (transactionStatus == TransactionStatus.SUCCESS || transactionStatus == TransactionStatus.FAILED)
                Single.just(transactionStatus)
            else {
                blockchain.transactionExist(transactionHash).flatMap { exist ->
                    if (exist)
                        Single.just(TransactionStatus.PENDING)
                    else
                        Single.just(TransactionStatus.NOTFOUND)
                }
            }
        }
    }

    fun estimateGas(contractAddress: String, value: BigInteger?, gasLimit: Long? = null, gasPrice: Long? = null,
                    data: ByteArray? = null): Single<Long> {
        return blockchain.estimateGas(receiveAddress, to = contractAddress, value = value, gasLimit = gasLimit,
                                      gasPrice = gasPrice, data = data)
    }

    @Throws(ValidationError::class)
    private fun convertValue(value: String): BigInteger {
        try {
            return value.toBigInteger()
        } catch (e: Exception) {
            throw ValidationError.InvalidValue
        }
    }

    fun send(toAddress: String, value: String, gasPrice: Long, gasLimit: Long): Single<TransactionInfo> {
        return send(toAddress.hexStringToByteArray(), convertValue(value), ByteArray(0), gasPrice, gasLimit)
    }

    fun send(toAddress: ByteArray, value: BigInteger, transactionInput: ByteArray, gasPrice: Long,
             gasLimit: Long): Single<TransactionInfo> {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, toAddress, value, transactionInput)
        logger.info("send rawTransaction: $rawTransaction")

        val fee = gasPrice.toBigInteger().times(gasLimit.toBigInteger())
        val totalValue = value.add(fee)

        logger.info("fee = $fee, total value = $totalValue, balance = $balance")

        return blockchain.send(rawTransaction)
                .doOnSuccess { transaction ->
                    transactionManager.handle(transaction)
                }
                .map { TransactionInfo(it) }
    }

    fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long,
                pullTimestamps: Boolean): Single<List<EthereumLog>> {
        return blockchain.getLogs(address, topics, fromBlock, toBlock, pullTimestamps)
    }

    fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray> {
        return blockchain.getStorageAt(contractAddress, position, blockNumber)
    }

    fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long? = null): Single<ByteArray> {
        return blockchain.call(contractAddress, data, blockNumber)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: ${address}")
        return lines.joinToString { "\n" }
    }

    fun statusInfo(): Map<String, Any> {
        val statusInfo = LinkedHashMap<String, Any>()

        statusInfo["Last Block Height"] = state.lastBlockHeight ?: "N/A"
        statusInfo["Sync State"] = blockchain.syncState
        statusInfo["Blockchain source"] = blockchain.source
        statusInfo["Transactions source"] = transactionManager.source

        return statusInfo
    }

    //
    //IBlockchain
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
        if (state.balance == balance)
            return

        state.balance = balance
        balanceSubject.onNext(balance)
    }

    //
    //TransactionManager.Listener
    //

    override fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        if (ethereumTransactions.isEmpty())
            return

        transactionsSubject.onNext(ethereumTransactions.map { tx -> TransactionInfo(tx) })
    }

    sealed class SyncState {
        class Synced : SyncState()
        class NotSynced : SyncState()
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

    companion object {
        fun getInstance(context: Context, privateKey: BigInteger, syncMode: SyncMode, networkType: NetworkType,
                        infuraCredentials: InfuraCredentials, etherscanKey: String, walletId: String): EthereumKit {
            val blockchain: IBlockchain

            val publicKey =
                    CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            val address = CryptoUtils.sha3(publicKey).takeLast(20).toByteArray()

            val network = networkType.getNetwork()
            val transactionSigner = TransactionSigner(network, privateKey)
            val transactionBuilder = TransactionBuilder(address)
            val rpcApiProvider = InfuraRpcApiProvider.getInstance(networkType, infuraCredentials, address)

            when (syncMode) {
                is SyncMode.ApiSyncMode -> {
                    val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(context, walletId, networkType)
                    val storage = ApiStorage(apiDatabase)

                    blockchain =
                            ApiBlockchain.getInstance(storage, transactionSigner, transactionBuilder, rpcApiProvider,
                                                      ConnectionManager(context))
                }
                is SyncMode.SpvSyncMode -> {
                    val spvDatabase = EthereumDatabaseManager.getEthereumSpvDatabase(context, walletId, networkType)
                    val nodeKey = CryptoUtils.ecKeyFromPrivate(syncMode.nodePrivateKey)
                    val storage = SpvStorage(spvDatabase)

                    blockchain =
                            SpvBlockchain.getInstance(storage, transactionSigner, transactionBuilder, rpcApiProvider,
                                                      network, address, nodeKey)
                }
                is SyncMode.GethSyncMode -> {
                    throw Exception("Geth Sync Mode not supported!")
//                    val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(context, walletId, networkType)
//                    val storage = ApiStorage(apiDatabase)
//                    val nodeDirectory = context.filesDir.path + "/gethNode"
//
//                    blockchain = GethBlockchain.getInstance(nodeDirectory, network, storage, transactionSigner, transactionBuilder, address)
                }
            }

            val transactionsProvider = TransactionsProvider.getInstance(networkType, etherscanKey, address)
            val transactionDatabase = EthereumDatabaseManager.getTransactionDatabase(context, walletId, networkType)
            val transactionStorage: ITransactionStorage = TransactionStorage(transactionDatabase)
            val transactionManager = TransactionManager(transactionStorage, transactionsProvider)

            val addressValidator = AddressValidator()

            val ethereumKit = EthereumKit(blockchain, transactionManager, addressValidator, transactionBuilder, address,
                                          networkType, walletId, etherscanKey)

            blockchain.listener = ethereumKit
            transactionManager.listener = ethereumKit

            return ethereumKit
        }

        fun getInstance(context: Context, words: List<String>, wordsSyncMode: WordsSyncMode, networkType: NetworkType,
                        infuraCredentials: InfuraCredentials, etherscanKey: String, walletId: String): EthereumKit {
            val seed = Mnemonic().toSeed(words)
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

            return getInstance(context, privateKey, syncMode, networkType, infuraCredentials, etherscanKey, walletId)
        }

        fun clear(context: Context, networkType: NetworkType, walletId: String) {
            EthereumDatabaseManager.clear(context, networkType, walletId)
        }
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
