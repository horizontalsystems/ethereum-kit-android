package io.horizontalsystems.ethereumkit

import android.content.Context
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.storage.RoomStorage
import io.horizontalsystems.ethereumkit.models.TransactionRoom
import io.reactivex.Single
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

class EthereumKitModule

class EthereumKit(
        private val blockchain: IBlockchain,
        val storage: IStorage,
        private val addressValidator: AddressValidator,
        private val state: State) : IBlockchainListener {

    interface Listener {
        fun onTransactionsUpdate(transactions: List<TransactionRoom>)
        fun onBalanceUpdate()
        fun onLastBlockHeightUpdate()
        fun onStateUpdate()
    }

    var listener: Listener? = null

    init {
        state.balance = storage.getBalance(blockchain.ethereumAddress) ?: BigDecimal.ZERO
        state.lastBlockHeight = storage.getLastBlockHeight()
    }

    fun start() {
        if (state.isSyncing) {
            return
        }

        blockchain.start()
    }

    fun stop() {
        blockchain.stop()
    }

    fun clear() {
        blockchain.clear()
        state.clear()
        storage.clear()
    }

    val receiveAddress: String
        get() {
            return blockchain.ethereumAddress
        }


    fun register(contractAddress: String, decimal: Int, listener: Listener) {
        if (state.hasContract(contractAddress)) {
            return
        }

        state.add(contractAddress, decimal, listener)

        blockchain.register(contractAddress, decimal)
    }

    fun unregister(contractAddress: String) {
        blockchain.unregister(contractAddress)
        state.remove(contractAddress)
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPrice: BigDecimal? = null): BigDecimal {
        return (gasPrice ?: blockchain.gasPrice) * blockchain.ethereumGasLimit.toBigDecimal()
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<TransactionRoom>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    fun send(toAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        blockchain.send(toAddress, amount, gasPrice, onSuccess, onError)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: ${blockchain.ethereumAddress}")
        return lines.joinToString { "\n" }
    }

    val balance: BigDecimal
        get() {
            return state.balance ?: BigDecimal.valueOf(0.0)
        }

    val syncState: SyncState
        get() {
            return state.syncState ?: SyncState.NotSynced
        }

    val lastBlockHeight: Int?
        get() {
            return state.lastBlockHeight
        }


    //
    // ERC20
    //

    fun feeERC20(gasPrice: BigDecimal? = null): BigDecimal {
        return (gasPrice ?: blockchain.gasPrice) * blockchain.erc20GasLimit.toBigDecimal()
    }

    fun balanceERC20(contractAddress: String): BigDecimal {
        return state.balance(contractAddress) ?: BigDecimal.valueOf(0.0)
    }

    fun syncStateErc20(contractAddress: String): SyncState {
        return state.state(contractAddress) ?: SyncState.NotSynced
    }

    fun transactionsERC20(contractAddress: String, fromHash: String? = null, limit: Int? = null): Single<List<TransactionRoom>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    fun sendERC20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)? = null, onError: (() -> Unit)? = null) {
        blockchain.sendErc20(toAddress, contractAddress, amount, gasPrice, onSuccess, onError)
    }

    //
    //IBlockchain
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Int) {
        state.lastBlockHeight = lastBlockHeight
        listener?.onLastBlockHeightUpdate()
        for (erc20Listener in state.erc20Listeners) {
            erc20Listener.onLastBlockHeightUpdate()
        }
    }

    override fun onUpdateState(syncState: SyncState) {
        state.syncState = syncState
    }

    override fun onUpdateErc20State(syncState: SyncState, contractAddress: String) {
        state.setSyncState(syncState, contractAddress)
        state.listener(contractAddress)?.onStateUpdate()
    }

    override fun onUpdateBalance(balance: BigDecimal) {
        state.balance = balance
        listener?.onBalanceUpdate()
    }

    override fun onUpdateErc20Balance(balance: BigDecimal, contractAddress: String) {
        state.setBalance(balance, contractAddress)
        state.listener(contractAddress)?.onBalanceUpdate()
    }

    override fun onUpdateTransactions(transactions: List<TransactionRoom>) {
        listener?.onTransactionsUpdate(transactions)
    }

    override fun onUpdateErc20Transactions(transactions: List<TransactionRoom>, contractAddress: String) {
        state.listener(contractAddress)?.onTransactionsUpdate(transactions)
    }


    open class EthereumKitException(msg: String) : Exception(msg) {
        class InfuraApiKeyNotSet : EthereumKitException("Infura API Key is not set!")
        class EtherscanApiKeyNotSet : EthereumKitException("Etherscan API Key is not set!")
        class FailedToLoadMetaData(errMsg: String?) : EthereumKitException("Failed to load meta-data, NameNotFound: $errMsg")
        class TokenNotFound(contract: String) : EthereumKitException("ERC20 token not found with contract: $contract")
    }

    sealed class SyncState {
        object Synced : SyncState()
        object NotSynced : SyncState()
        object Syncing : SyncState()
    }

    companion object {
        fun ethereumKit(context: Context, seed: ByteArray, walletId: String, testMode: Boolean, infuraKey: String, etherscanKey: String): EthereumKit {

            val storage = RoomStorage("ethereumKit-$testMode-$walletId", context)
            val blockchain = Web3jBlockchain(storage, seed, testMode, infuraKey, etherscanKey)
            val addressValidator = AddressValidator()

            val ethereumKit = EthereumKit(blockchain, storage, addressValidator, State())
            blockchain.listener = ethereumKit

            return ethereumKit
        }

    }

    class State {

        var balance: BigDecimal? = null
        var lastBlockHeight: Int? = null
        var syncState: SyncState? = null

        val erc20List = ConcurrentHashMap<String, ERC20>()

        val erc20Listeners: List<Listener>
            get() {
                val listeners = mutableListOf<Listener>()
                erc20List.values.forEach {
                    listeners.add(it.listener)
                }
                return listeners
            }

        val isSyncing: Boolean
            get() {
                if (syncState == SyncState.Syncing) {
                    return true
                }
                erc20List.values.forEach {
                    if (it.syncState == SyncState.Syncing) {
                        return true
                    }
                }
                return false
            }

        fun clear() {
            balance = null
            lastBlockHeight = null
            syncState = SyncState.NotSynced
            erc20List.clear()
        }

        fun add(contractAddress: String, decimal: Int, listener: Listener) {
            erc20List[contractAddress] = ERC20(contractAddress, decimal, listener)
        }

        fun hasContract(contractAddress: String): Boolean {
            return erc20List.containsKey(contractAddress)
        }

        fun remove(contractAddress: String) {
            erc20List.remove(contractAddress)
        }

        fun balance(contractAddress: String): BigDecimal? {
            return erc20List[contractAddress]?.balance
        }

        fun state(contractAddress: String): SyncState? {
            return erc20List[contractAddress]?.syncState
        }

        fun listener(contractAddress: String): Listener? {
            return erc20List[contractAddress]?.listener
        }

        fun setBalance(balance: BigDecimal?, contractAddress: String) {
            erc20List[contractAddress]?.balance = balance
        }

        fun setSyncState(syncState: SyncState?, contractAddress: String) {
            erc20List[contractAddress]?.syncState = syncState
        }
    }

}
