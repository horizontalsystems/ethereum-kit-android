package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import java.math.BigInteger


data class TransactionKey(val hash: ByteArray, val interTransactionIndex: Int)

interface ITransactionManagerListener {
    fun onSyncSuccess(transactions: List<Transaction>)
    fun onSyncTransactionsError(error: Throwable)
}

interface ITransactionManager {
    var listener: ITransactionManagerListener?

    fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>>
    fun sync()
    fun send(to: Address, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction>
    fun getTransactionInput(to: Address, value: BigInteger): ByteArray
}

interface IBalanceManagerListener {
    fun onSyncBalanceSuccess(balance: BigInteger)
    fun onSyncBalanceError(error: Throwable)
}

interface IBalanceManager {
    var listener: IBalanceManagerListener?

    val balance: BigInteger?
    fun sync()
}

interface ITransactionStorage {
    val lastTransactionBlockHeight: Long?

    fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>>
    fun getPendingTransactions(): List<Transaction>
    fun save(transactions: List<Transaction>)
}

interface ITransactionsProvider {
    fun getTransactions(contractAddress: Address, address: Address, startBlock: Long, endBlock: Long): Single<List<Transaction>>
}

interface ITokenBalanceStorage {
    fun getBalance(): BigInteger?
    fun save(balance: BigInteger)
}

interface IDataProvider {
    val lastBlockHeight: Long

    fun getTransactionLogs(contractAddress: Address, address: Address, from: Long, to: Long): Single<List<EthereumLog>>
    fun getBalance(contractAddress: Address, address: Address): Single<BigInteger>
    fun send(contractAddress: Address, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray>
    fun getTransactionStatuses(transactionHashes: List<ByteArray>): Single<Map<ByteArray, TransactionStatus>>
}

interface ITransactionBuilder {
    fun transferTransactionInput(to: Address, value: BigInteger): ByteArray
}
