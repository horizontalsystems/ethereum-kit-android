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

class EthereumKitModule

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

    init {
        state.balance = storage.getBalance(blockchain.ethereumAddress) ?: BigDecimal.ZERO
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


    fun register(contractAddress: String, decimal: Int, listener: Listener) {
        if (state.hasContract(contractAddress)) {
            return
        }

        state.add(contractAddress, decimal, listener)
        state.setBalance(storage.getBalance(contractAddress)
                ?: BigDecimal.valueOf(0.0), contractAddress)

        blockchain.register(contractAddress, decimal)
    }

    fun unregister(contractAddress: String) {
        blockchain.unregister(contractAddress)
        state.remove(contractAddress)
    }

    fun validateAddress(address: String) {
        addressValidator.validate(address)
    }

    fun fee(gasPriceInWei: Long? = null): BigDecimal {
        val gas = BigDecimal.valueOf(gasPriceInWei ?: blockchain.gasPriceInWei)

        return Convert.fromWei(gas.multiply(blockchain.gasLimitEthereum.toBigDecimal()), Convert.Unit.ETHER)
    }

    fun transactions(fromHash: String? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    fun send(toAddress: String, amount: BigDecimal, gasPriceInWei: Long? = null): Single<EthereumTransaction> {
        return blockchain.send(toAddress, amount, gasPriceInWei)
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

    val lastBlockHeight: Int?
        get() {
            return state.lastBlockHeight
        }

    val syncState: SyncState
        get() {
            return blockchain.blockchainSyncState
        }


    //
    // ERC20
    //

    fun feeERC20(gasPriceInWei: Long? = null): BigDecimal {
        val gas = BigDecimal.valueOf(gasPriceInWei ?: blockchain.gasPriceInWei)

        return Convert.fromWei(gas.multiply(blockchain.gasLimitErc20.toBigDecimal()), Convert.Unit.ETHER)
    }

    fun balanceERC20(contractAddress: String): BigDecimal {
        return state.balance(contractAddress) ?: BigDecimal.valueOf(0.0)
    }

    fun syncStateErc20(contractAddress: String): SyncState {
        return blockchain.syncState(contractAddress)
    }

    fun transactionsERC20(contractAddress: String, fromHash: String? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    fun sendERC20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPriceInWei: Long? = null): Single<EthereumTransaction> {
        return blockchain.sendErc20(toAddress, contractAddress, amount, gasPriceInWei)
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
        listener?.onSyncStateUpdate()
    }

    override fun onUpdateErc20State(syncState: SyncState, contractAddress: String) {
        state.listener(contractAddress)?.onSyncStateUpdate()
    }

    override fun onUpdateBalance(balance: BigDecimal) {
        state.balance = balance
        listener?.onBalanceUpdate()
    }

    override fun onUpdateErc20Balance(balance: BigDecimal, contractAddress: String) {
        state.setBalance(balance, contractAddress)
        state.listener(contractAddress)?.onBalanceUpdate()
    }

    override fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        listener?.onTransactionsUpdate(ethereumTransactions)
    }

    override fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: String) {
        state.listener(contractAddress)?.onTransactionsUpdate(ethereumTransactions)
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
