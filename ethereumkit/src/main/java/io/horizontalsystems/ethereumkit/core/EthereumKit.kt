package io.horizontalsystems.ethereumkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.api.ApiBlockchain
import io.horizontalsystems.ethereumkit.api.models.EthereumKitState
import io.horizontalsystems.ethereumkit.api.storage.ApiRoomStorage
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.spv.core.SpvBlockchain
import io.horizontalsystems.ethereumkit.spv.core.SpvRoomStorage
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class EthereumKit(
        private val blockchain: IBlockchain,
        private val addressValidator: AddressValidator,
        private val transactionBuilder: TransactionBuilder,
        private val state: EthereumKitState = EthereumKitState()) : IBlockchainListener {

    interface Listener {
        fun onClear() {}

        fun onTransactionsUpdate(transactions: List<TransactionInfo>) {}
        fun onBalanceUpdate() {}
        fun onLastBlockHeightUpdate() {}
        fun onSyncStateUpdate() {}
    }

    private var listeners: MutableList<Listener> = mutableListOf()
    private var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    private val gasLimit: Long = 21_000

    init {
        state.balance = blockchain.balance
        state.lastBlockHeight = blockchain.lastBlockHeight
    }

    fun start() {
        blockchain.start()
    }

    fun stop() {
        blockchain.stop()
    }

    fun clear() {
        listeners.clear()

        blockchain.clear()
        state.clear()
    }

    val lastBlockHeight: Long?
        get() = state.lastBlockHeight

    val balance: BigInteger?
        get() = state.balance

    val syncState: SyncState
        get() = blockchain.syncState

    val receiveAddress: String
        get() = blockchain.address.toEIP55Address()

    val receiveAddressRaw: ByteArray
        get() = blockchain.address

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPrice: Long): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimit.toBigDecimal())
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionInfo>> {
        return blockchain.getTransactions(fromHash?.hexStringToByteArray(), limit)
                .map { txs -> txs.map { TransactionInfo(it) } }
    }

    fun send(toAddress: String, value: BigInteger, gasPrice: Long): Single<EthereumTransaction> {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, toAddress.hexStringToByteArray(), value)

        return blockchain.send(rawTransaction)
    }

    fun send(toAddress: ByteArray, value: BigInteger, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long = this.gasLimit): Single<TransactionInfo> {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, toAddress, value, transactionInput)
        return blockchain.send(rawTransaction)
                .map { TransactionInfo(it) }
    }

    fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
        return blockchain.getLogs(address, topics, fromBlock, toBlock, pullTimestamps)
    }

    fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray> {
        return blockchain.getStorageAt(contractAddress, position, blockNumber)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: ${blockchain.address}")
        return lines.joinToString { "\n" }
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    //
    //IBlockchain
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        state.lastBlockHeight = lastBlockHeight
        listenerExecutor.execute {
            listeners.forEach { it.onLastBlockHeightUpdate() }
        }
    }

    override fun onUpdateSyncState(syncState: SyncState) {
        listenerExecutor.execute {
            listeners.forEach { it.onSyncStateUpdate() }
        }
    }

    override fun onUpdateBalance(balance: BigInteger) {
        state.balance = balance
        listenerExecutor.execute {
            listeners.forEach { it.onBalanceUpdate() }
        }
    }

    override fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        if (ethereumTransactions.isEmpty())
            return

        listenerExecutor.execute {
            listeners.forEach { it.onTransactionsUpdate(ethereumTransactions.map { tx -> TransactionInfo(tx) }) }
        }
    }

    sealed class SyncState {
        object Synced : SyncState()
        object NotSynced : SyncState()
        object Syncing : SyncState()
    }

    companion object {
        fun getInstance(context: Context, privateKey: BigInteger, syncMode: SyncMode, networkType: NetworkType, walletId: String): EthereumKit {
            val blockchain: IBlockchain

            val publicKey = CryptoUtils.ecKeyFromPrivate(privateKey).publicKeyPoint.getEncoded(false).drop(1).toByteArray()
            val address = CryptoUtils.sha3(publicKey).takeLast(20).toByteArray()

            val network = networkType.getNetwork()
            val transactionSigner = TransactionSigner(network, privateKey)
            val transactionBuilder = TransactionBuilder()

            when (syncMode) {
                is SyncMode.ApiSyncMode -> {
                    val storage = ApiRoomStorage("api-$walletId-${networkType.name}", context)
                    blockchain = ApiBlockchain.getInstance(storage, networkType, transactionSigner, transactionBuilder, address, syncMode.infuraKey, syncMode.etherscanKey)
                }
                is SyncMode.SpvSyncMode -> {
                    val nodeKey = CryptoUtils.ecKeyFromPrivate(syncMode.nodePrivateKey)
                    val storage = SpvRoomStorage(context, "spv-$walletId-${networkType.name}")

                    blockchain = SpvBlockchain.getInstance(storage, transactionSigner, transactionBuilder, network, address, nodeKey)
                }
            }

            val addressValidator = AddressValidator()

            val ethereumKit = EthereumKit(blockchain, addressValidator, transactionBuilder)

            blockchain.listener = ethereumKit

            return ethereumKit
        }
    }

    sealed class SyncMode {
        class SpvSyncMode(val nodePrivateKey: BigInteger) : SyncMode()
        class ApiSyncMode(val infuraKey: String, val etherscanKey: String) : SyncMode()
    }

    enum class NetworkType {
        MainNet,
        Ropsten,
        Kovan,
        Rinkeby;

        fun getNetwork(): INetwork {
            return io.horizontalsystems.ethereumkit.spv.net.Ropsten()
        }
    }

}
