package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.TransactionCache
import io.horizontalsystems.ethereumkit.models.Address
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
    fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<TransactionCache>>
    fun getPendingTransactions(): List<TransactionCache>
    fun save(transaction: TransactionCache)
    fun getLastTransaction(): TransactionCache?
}

interface ITokenBalanceStorage {
    fun getBalance(): BigInteger?
    fun save(balance: BigInteger)
}

interface IDataProvider {
    fun getBalance(contractAddress: Address, address: Address): Single<BigInteger>
}
