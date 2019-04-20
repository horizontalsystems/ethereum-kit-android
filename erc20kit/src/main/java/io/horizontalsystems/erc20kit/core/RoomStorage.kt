package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single

class RoomStorage(context: Context, databaseName: String) : ITransactionStorage, ITokenBalanceStorage {

    private val database = Erc20KitDatabase.getInstance(context, databaseName)


    // ITransactionStorage

    override val lastTransactionBlockHeight: Long?
        get() = database.transactionDao.getLastTransaction()?.blockNumber

    override fun lastTransactionBlockHeight(contractAddress: ByteArray): Long? {
        return database.transactionDao.getLastTransaction(contractAddress)?.blockNumber
    }

    override fun getTransactions(contractAddress: ByteArray, hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>> {
        return database.transactionDao.getAllTransactions(contractAddress).flatMap { transactionsList ->
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

    override fun update(transaction: Transaction) {
        database.transactionDao.update(transaction)
    }

    override fun clearTransactions() {
        database.transactionDao.deleteAll()
    }


    // ITokenBalanceStorage

    override fun getTokenBalance(contractAddress: ByteArray): TokenBalance? {
        return database.tokenBalanceDao.getTokenBalance(contractAddress)
    }

    override fun save(tokenBalance: TokenBalance) {
        database.tokenBalanceDao.insert(tokenBalance)
    }

    override fun clearTokenBalances() {
        database.tokenBalanceDao.deleteAll()
    }
}
