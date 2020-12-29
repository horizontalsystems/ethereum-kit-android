package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.TransactionRecord
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import java.math.BigInteger


data class TransactionKey(val hash: ByteArray, val interTransactionIndex: Int)

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
    fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<TransactionRecord>>
    fun getPendingTransactions(): List<TransactionRecord>
    fun save(transaction: TransactionRecord)
    fun getLastTransaction(): TransactionRecord?
}

interface ITokenBalanceStorage {
    fun getBalance(): BigInteger?
    fun save(balance: BigInteger)
}

interface IDataProvider {
    val lastBlockHeight: Long

    fun getBalance(contractAddress: Address, address: Address): Single<BigInteger>
    fun send(contractAddress: Address, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray>
    fun getTransactionStatuses(transactionHashes: List<ByteArray>): Single<Map<ByteArray, TransactionStatus>>
}