package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.reactivex.Single
import java.math.BigInteger

interface IErc20TokenListener {
    fun onUpdate(transactions: List<TransactionInfo>)
    fun onUpdateBalance()
    fun onUpdateSyncState()
}

interface ITransactionManagerListener {
    fun onSyncSuccess(transactions: List<Transaction>)
    fun onSyncTransactionsError()
}

interface ITransactionManager {
    var listener: ITransactionManagerListener?

    fun lastTransactionBlockHeight(contractAddress: ByteArray): Long?
    fun transactionsSingle(contractAddress: ByteArray, hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>>

    fun sync()
    fun sendSingle(contractAddress: ByteArray, to: ByteArray, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction>

    fun clear()
}

interface IBalanceManagerListener {
    fun onBalanceUpdate(balance: TokenBalance, contractAddress: ByteArray)
    fun onSyncBalanceSuccess(contractAddress: ByteArray)
    fun onSyncBalanceError(contractAddress: ByteArray)
}

interface IBalanceManager {
    var listener: IBalanceManagerListener?

    fun balance(contractAddress: ByteArray): TokenBalance
    fun sync(blockHeight: Long, contractAddress: ByteArray, balancePosition: Int)

    fun clear()
}

interface ITokenHolder {
    val contractAddresses: List<ByteArray>

    fun syncState(contractAddress: ByteArray): Erc20Kit.SyncState
    fun balance(contractAddress: ByteArray): TokenBalance
    fun balancePosition(contractAddress: ByteArray): Int

    fun listener(contractAddress: ByteArray): IErc20TokenListener?
    fun register(contractAddress: ByteArray, balancePosition: Int, balance: TokenBalance, listener: IErc20TokenListener)
    fun unregister(contractAddress: ByteArray)
    fun set(syncState: Erc20Kit.SyncState, contractAddress: ByteArray)
    fun set(balance: TokenBalance, contractAddress: ByteArray)
    fun clear()
}

interface ITransactionStorage {
    val lastTransactionBlockHeight: Long?
    fun lastTransactionBlockHeight(contractAddress: ByteArray): Long?

    fun getTransactions(contractAddress: ByteArray, hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>>
    fun save(transactions: List<Transaction>)
    fun update(transaction: Transaction)
    fun clearTransactions()
}

interface ITokenBalanceStorage {
    fun getTokenBalance(contractAddress: ByteArray): TokenBalance?
    fun save(tokenBalance: TokenBalance)
    fun clearTokenBalances()
}

interface IDataProvider {
    val lastBlockHeight: Long

    fun getTransactions(from: Long, to: Long, address: ByteArray): Single<List<Transaction>>
    fun getStorageValue(contractAddress: ByteArray, position: Int, address: ByteArray, blockHeight: Long): Single<BigInteger>
    fun sendSingle(contractAddress: ByteArray, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray>
}

interface ITransactionBuilder {
    fun transferTransactionInput(toAddress: ByteArray, value: BigInteger): ByteArray
}
