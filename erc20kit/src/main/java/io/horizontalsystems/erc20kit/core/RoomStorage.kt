package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single
import java.math.BigInteger

class RoomStorage(context: Context, databaseName: String) : ITransactionStorage, ITokenBalanceStorage {

    private val database = Erc20KitDatabase.getInstance(context, databaseName)


    // ITransactionStorage

    override val lastTransactionBlockHeight: Long?
        get() = database.transactionDao.getLastTransaction()?.blockNumber


    override fun getTransactions(hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>> {
        return database.transactionDao.getAllTransactions().flatMap { transactionsList ->
            var transactions = transactionsList

            hashFrom?.let { hashFrom ->
                val tx = transactions.firstOrNull { it.transactionHash.contentEquals(hashFrom) }
                tx?.timestamp?.let { txTimeStamp ->
                    transactions = transactions.filter { indexFrom != null && it.timestamp == txTimeStamp && it.logIndex < indexFrom || it.timestamp < txTimeStamp }
                }
            }
            limit?.let {
                transactions = transactions.take(it)
            }

            Single.just(transactions)
        }
    }

    override fun save(transactions: List<Transaction>) {
        database.transactionDao.insert(transactions)
    }

    override fun clearTransactions() {
        database.transactionDao.deleteAll()
    }


    // ITokenBalanceStorage

    override fun getBalance(): BigInteger? {
        return database.tokenBalanceDao.getBalance()?.value
    }

    override fun save(balance: BigInteger) {
        database.tokenBalanceDao.insert(TokenBalance(balance))
    }

    override fun clearBalance() {
        database.tokenBalanceDao.deleteAll()
    }
}
