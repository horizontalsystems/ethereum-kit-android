package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger


data class TransactionKey(val hash: ByteArray, val interTransactionIndex: Int)

interface ITransactionManagerListener {
    fun onSyncSuccess(transactions: List<Transaction>)
    fun onSyncTransactionsError()
}

interface ITransactionManager {
    var listener: ITransactionManagerListener?

    val lastTransactionBlockHeight: Long?
    fun transactionsSingle(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>>
    fun sync()
    fun sendSingle(to: ByteArray, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction>
    fun clear()
}

interface IBalanceManagerListener {
    fun onSyncBalanceSuccess(balance: BigInteger)
    fun onSyncBalanceError()
}

interface IBalanceManager {
    var listener: IBalanceManagerListener?

    val balance: BigInteger?
    fun sync()
    fun clear()
}

interface ITokenHolder {
    val contractAddresses: List<ByteArray>

    fun syncState(contractAddress: ByteArray): Erc20Kit.SyncState
    fun balance(contractAddress: ByteArray): TokenBalance
    fun balancePosition(contractAddress: ByteArray): Int

    fun syncStateSubject(contractAddress: ByteArray): PublishSubject<Erc20Kit.SyncState>
    fun balanceSubject(contractAddress: ByteArray): PublishSubject<BigInteger>
    fun transactionsSubject(contractAddress: ByteArray): PublishSubject<List<TransactionInfo>>

    fun register(contractAddress: ByteArray, balancePosition: Int, balance: TokenBalance)
    fun unregister(contractAddress: ByteArray)
    fun set(syncState: Erc20Kit.SyncState, contractAddress: ByteArray)
    fun set(balance: TokenBalance, contractAddress: ByteArray)
    fun clear()
}

interface ITransactionStorage {
    val lastTransactionBlockHeight: Long?

    fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>>
    fun getPendingTransactions(): List<Transaction>
    fun save(transactions: List<Transaction>)
    fun clearTransactions()
}

interface ITokenBalanceStorage {
    fun getBalance(): BigInteger?
    fun save(balance: BigInteger)
    fun clearBalance()
}

interface IDataProvider {
    val lastBlockHeight: Long

    fun getTransactionLogs(contractAddress: ByteArray, address: ByteArray, from: Long, to: Long): Single<List<EthereumLog>>
    fun getBalance(contractAddress: ByteArray, address: ByteArray): Single<BigInteger>
    fun sendSingle(contractAddress: ByteArray, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray>
}

interface ITransactionBuilder {
    fun transferTransactionInput(toAddress: ByteArray, value: BigInteger): ByteArray
}
