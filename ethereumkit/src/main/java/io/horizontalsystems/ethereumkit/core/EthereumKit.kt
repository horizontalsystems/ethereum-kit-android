package io.horizontalsystems.ethereumkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.api.ApiBlockchain
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiStorage
import io.horizontalsystems.ethereumkit.core.storage.TransactionStorage
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.TransactionInfo
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
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.BigInteger

class EthereumKit(
        private val blockchain: IBlockchain,
        private val transactionManager: TransactionManager,
        private val addressValidator: AddressValidator,
        private val transactionBuilder: TransactionBuilder,
        private val address: ByteArray,
        private val state: EthereumKitState = EthereumKitState()) : IBlockchainListener, TransactionManager.Listener {

    private val logger = LoggerFactory.getLogger(EthereumKit::class.java)

    private val lastBlockHeightSubject = PublishSubject.create<Long>()
    private val syncStateSubject = PublishSubject.create<SyncState>()
    private val balanceSubject = PublishSubject.create<BigInteger>()
    private val transactionsSubject = PublishSubject.create<List<TransactionInfo>>()

    private val gasLimit: Long = 21_000

    init {
        state.balance = blockchain.balance
        state.lastBlockHeight = blockchain.lastBlockHeight
    }

    fun start() {
        blockchain.start()
        transactionManager.refresh()
    }

    fun stop() {
        blockchain.stop()
        state.clear()
    }

    fun refresh() {
        blockchain.refresh()
        transactionManager.refresh()
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

    fun send(toAddress: String, value: String, gasPrice: Long): Single<TransactionInfo> {
        return send(toAddress.hexStringToByteArray(), value.toBigInteger(), ByteArray(0), gasPrice, gasLimit)
    }

    fun send(toAddress: ByteArray, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<TransactionInfo> {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, toAddress, value, transactionInput)
        logger.debug("send rawTransaction: {}", rawTransaction)

        val fee = gasPrice.toBigInteger().times(gasLimit.toBigInteger())
        val totalValue = value.add(fee)

        logger.debug("fee = $fee, total value = $totalValue, balance = $balance")

        return blockchain.send(rawTransaction)
                .doOnSuccess { transaction ->
                    transactionManager.handle(transaction)
                }
                .map { TransactionInfo(it) }
    }

    fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
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

    val lastBlockHeightFlowable: Flowable<Long>
        get() = lastBlockHeightSubject.toFlowable(BackpressureStrategy.BUFFER)

    val syncStateFlowable: Flowable<SyncState>
        get() = syncStateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val balanceFlowable: Flowable<BigInteger>
        get() = balanceSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsFlowable: Flowable<List<TransactionInfo>>
        get() = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

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
    }

    companion object {
        fun getInstance(context: Context, privateKey: BigInteger, syncMode: SyncMode, networkType: NetworkType, infuraCredentials: InfuraCredentials, etherscanKey: String, walletId: String): EthereumKit {
            val blockchain: IBlockchain

            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            val address = CryptoUtils.sha3(publicKey).takeLast(20).toByteArray()

            val network = networkType.getNetwork()
            val transactionSigner = TransactionSigner(network, privateKey)
            val transactionBuilder = TransactionBuilder(address)
            val rpcApiProvider = RpcApiProvider.getInstance(networkType, infuraCredentials, address)

            when (syncMode) {
                is SyncMode.ApiSyncMode -> {
                    val apiDatabase = EthereumDatabaseManager.getEthereumApiDatabase(context, walletId, networkType)
                    val storage = ApiStorage(apiDatabase)

                    blockchain = ApiBlockchain.getInstance(storage, transactionSigner, transactionBuilder, rpcApiProvider)
                }
                is SyncMode.SpvSyncMode -> {
                    val spvDatabase = EthereumDatabaseManager.getEthereumSpvDatabase(context, walletId, networkType)
                    val nodeKey = CryptoUtils.ecKeyFromPrivate(syncMode.nodePrivateKey)
                    val storage = SpvStorage(spvDatabase)

                    blockchain = SpvBlockchain.getInstance(storage, transactionSigner, transactionBuilder, rpcApiProvider, network, address, nodeKey)
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

            val ethereumKit = EthereumKit(blockchain, transactionManager, addressValidator, transactionBuilder, address)

            blockchain.listener = ethereumKit

            return ethereumKit
        }

        fun getInstance(context: Context, words: List<String>, wordsSyncMode: WordsSyncMode, networkType: NetworkType, infuraCredentials: InfuraCredentials, etherscanKey: String, walletId: String): EthereumKit {
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

        fun clear(context: Context) {
            EthereumDatabaseManager.clear(context)
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
