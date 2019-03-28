package io.horizontalsystems.ethereumkit.core

import android.content.Context
import io.horizontalsystems.ethereumkit.api.ApiBlockchain
import io.horizontalsystems.ethereumkit.api.models.State
import io.horizontalsystems.ethereumkit.api.storage.ApiRoomStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
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
        private val state: State = State()) : IBlockchainListener {

    interface Listener {
        fun onTransactionsUpdate(transactions: List<EthereumTransaction>)
        fun onBalanceUpdate()
        fun onLastBlockHeightUpdate()
        fun onSyncStateUpdate()
    }

    var listener: Listener? = null
    var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    private val gasLimit: Long = 21_000
    private val gasLimitErc20: Long = 100_000

    init {
        state.balance = blockchain.balance
        state.lastBlockHeight = blockchain.getLastBlockHeight()
    }

    fun start() {
        blockchain.start()
    }

    fun stop() {
        blockchain.stop()
    }

    fun clear() {
        listener = null

        blockchain.clear()
        state.clear()
    }

    val receiveAddress: ByteArray
        get() {
            return blockchain.address
        }

    fun register(contractAddress: ByteArray, listener: Listener) {
        if (state.hasContract(contractAddress)) {
            return
        }

        state.add(contractAddress, listener)
        state.setBalance(blockchain.getBalanceErc20(contractAddress), contractAddress)

        blockchain.register(contractAddress)
    }

    fun unregister(contractAddress: ByteArray) {
        blockchain.unregister(contractAddress)
        state.remove(contractAddress)
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPrice: Long): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimit.toBigDecimal())
    }

    fun transactions(fromHash: ByteArray? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return blockchain.getTransactions(fromHash, limit)
    }

    fun send(toAddress: ByteArray, amount: BigInteger, gasPrice: Long): Single<EthereumTransaction> {
        val rawTransaction = transactionBuilder.rawTransaction(gasPrice, gasLimit, toAddress, amount)

        return blockchain.send(rawTransaction)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: ${blockchain.address}")
        return lines.joinToString { "\n" }
    }

    val balance: BigInteger?
        get() {
            return state.balance
        }

    val lastBlockHeight: Long?
        get() {
            return state.lastBlockHeight
        }

    val syncState: SyncState
        get() = blockchain.syncState

    //
    // ERC20
    //

    fun feeERC20(gasPrice: Long): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimitErc20.toBigDecimal())
    }

    fun balanceERC20(contractAddress: ByteArray): BigInteger? {
        return state.balance(contractAddress)
    }

    fun syncStateErc20(contractAddress: ByteArray): SyncState {
        return blockchain.getSyncStateErc20(contractAddress)
    }

    fun transactionsERC20(contractAddress: ByteArray, fromHash: ByteArray? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return blockchain.getTransactionsErc20(contractAddress, fromHash, limit)
    }

    fun sendERC20(toAddress: ByteArray, contractAddress: ByteArray, amount: BigInteger, gasPrice: Long): Single<EthereumTransaction> {
        val rawTransaction = transactionBuilder.rawErc20Transaction(contractAddress, gasPrice, gasLimitErc20, toAddress, amount)

        return blockchain.send(rawTransaction)
    }

    //
    //IBlockchain
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        state.lastBlockHeight = lastBlockHeight
        listenerExecutor.execute {
            listener?.onLastBlockHeightUpdate()
        }
        for (erc20Listener in state.erc20Listeners) {
            listenerExecutor.execute {
                erc20Listener.onLastBlockHeightUpdate()
            }
        }
    }

    override fun onUpdateSyncState(syncState: SyncState) {
        listenerExecutor.execute {
            listener?.onSyncStateUpdate()
        }
    }

    override fun onUpdateErc20SyncState(syncState: SyncState, contractAddress: ByteArray) {
        listenerExecutor.execute {
            state.listener(contractAddress)?.onSyncStateUpdate()
        }
    }

    override fun onUpdateBalance(balance: BigInteger) {
        state.balance = balance
        listenerExecutor.execute {
            listener?.onBalanceUpdate()
        }
    }

    override fun onUpdateErc20Balance(balance: BigInteger, contractAddress: ByteArray) {
        state.setBalance(balance, contractAddress)
        listenerExecutor.execute {
            state.listener(contractAddress)?.onBalanceUpdate()
        }
    }

    override fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        if (ethereumTransactions.isEmpty())
            return

        listenerExecutor.execute {
            listener?.onTransactionsUpdate(ethereumTransactions)
        }
    }

    override fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: ByteArray) {
        if (ethereumTransactions.isEmpty())
            return

        listenerExecutor.execute {
            state.listener(contractAddress)?.onTransactionsUpdate(ethereumTransactions)
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
                    blockchain = ApiBlockchain.getInstance(storage, transactionSigner, transactionBuilder, address)
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
