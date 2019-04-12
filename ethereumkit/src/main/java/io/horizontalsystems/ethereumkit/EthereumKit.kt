package io.horizontalsystems.ethereumkit

import android.content.Context
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.storage.RoomStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.State
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Single
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class EthereumKit(
        private val blockchain: IBlockchain,
        val storage: IStorage,
        private val addressValidator: AddressValidator,
        private val state: State) : IBlockchainListener {

    interface Listener {
        fun onTransactionsUpdate(transactions: List<EthereumTransaction>)
        fun onBalanceUpdate()
        fun onLastBlockHeightUpdate()
        fun onSyncStateUpdate()
    }

    var listener: Listener? = null
    var listenerExecutor: Executor = Executors.newSingleThreadExecutor()

    init {
        state.balance = storage.getBalance(blockchain.ethereumAddress)
        state.lastBlockHeight = storage.getLastBlockHeight()
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
        storage.clear()
    }

    val receiveAddress: String
        get() {
            return blockchain.ethereumAddress
        }


    fun register(contractAddress: String, listener: Listener) {
        if (state.hasContract(contractAddress)) {
            return
        }

        state.add(contractAddress, listener)
        state.setBalance(storage.getBalance(contractAddress), contractAddress)

        blockchain.register(contractAddress)
    }

    fun unregister(contractAddress: String) {
        blockchain.unregister(contractAddress)
        state.remove(contractAddress)
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPriceInWei: Long): BigDecimal {
        return Convert.fromWei(gasPriceInWei.toBigDecimal().multiply(blockchain.gasLimitEthereum.toBigDecimal()), Convert.Unit.ETHER)
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    fun send(toAddress: String, amount: String, gasPriceInWei: Long): Single<EthereumTransaction> {
        return blockchain.send(toAddress, amount, gasPriceInWei)
    }

    fun debugInfo(): String {
        val lines = mutableListOf<String>()
        lines.add("ADDRESS: ${blockchain.ethereumAddress}")
        return lines.joinToString { "\n" }
    }

    val balance: String?
        get() {
            return state.balance
        }

    val lastBlockHeight: Int?
        get() {
            return state.lastBlockHeight
        }

    val syncState: SyncState
        get() = blockchain.blockchainSyncState


    //
    // ERC20
    //

    fun feeERC20(gasPriceInWei: Long): BigDecimal {
        return Convert.fromWei(gasPriceInWei.toBigDecimal().multiply(blockchain.gasLimitErc20.toBigDecimal()), Convert.Unit.ETHER)
    }

    fun balanceERC20(contractAddress: String): String? {
        return state.balance(contractAddress)
    }

    fun syncStateErc20(contractAddress: String): SyncState {
        return blockchain.syncState(contractAddress)
    }

    fun transactionsERC20(contractAddress: String, fromHash: String? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    fun sendERC20(toAddress: String, contractAddress: String, amount: String, gasPriceInWei: Long): Single<EthereumTransaction> {
        return blockchain.sendErc20(toAddress, contractAddress, amount, gasPriceInWei)
    }

    //
    //IBlockchain
    //

    override fun onUpdateLastBlockHeight(lastBlockHeight: Int) {
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

    override fun onUpdateState(syncState: SyncState) {
        listenerExecutor.execute {
            listener?.onSyncStateUpdate()
        }
    }

    override fun onUpdateErc20State(syncState: SyncState, contractAddress: String) {
        listenerExecutor.execute {
            state.listener(contractAddress)?.onSyncStateUpdate()
        }
    }

    override fun onUpdateBalance(balance: String) {
        state.balance = balance
        listenerExecutor.execute {
            listener?.onBalanceUpdate()
        }
    }

    override fun onUpdateErc20Balance(balance: String, contractAddress: String) {
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

    override fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: String) {
        if (ethereumTransactions.isEmpty())
            return

        listenerExecutor.execute {
            state.listener(contractAddress)?.onTransactionsUpdate(ethereumTransactions)
        }
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
        fun ethereumKit(context: Context, words: List<String>, walletId: String, testMode: Boolean, infuraKey: String, etherscanKey: String): EthereumKit {
            return ethereumKit(context, Mnemonic().toSeed(words), walletId, testMode, infuraKey, etherscanKey)
        }

        fun ethereumKit(context: Context, seed: ByteArray, walletId: String, testMode: Boolean, infuraKey: String, etherscanKey: String): EthereumKit {

            val storage = RoomStorage("ethereumKit-$testMode-$walletId", context)
            val blockchain = ApiBlockchain.apiBlockchain(storage, seed, testMode, infuraKey, etherscanKey)
            val addressValidator = AddressValidator()

            val ethereumKit = EthereumKit(blockchain, storage, addressValidator, State())
            blockchain.listener = ethereumKit

            return ethereumKit
        }

    }

}
