package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single
import java.math.BigInteger

class Erc20Storage(private val database: Erc20KitDatabase) : ITransactionStorage, ITokenBalanceStorage {

    // ITransactionStorage

    override val lastTransactionBlockHeight: Long?
        get() = database.transactionDao.getLastTransaction()?.blockNumber


    override fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return database.transactionDao.getAllTransactions().flatMap { transactionsList ->
            var transactions = transactionsList

            fromTransaction?.let { fromTxKey ->
                transactions.firstOrNull { it.transactionHash.contentEquals(fromTxKey.hash) && it.interTransactionIndex == fromTxKey.interTransactionIndex }?.let { txFrom ->
                    transactions = transactions
                            .filter {
                                it.timestamp < txFrom.timestamp ||
                                        it.timestamp == txFrom.timestamp && (it.transactionIndex?.compareTo(txFrom.transactionIndex ?:0 ) ?: 0) < 0 ||
                                        it.timestamp == txFrom.timestamp && it.transactionIndex == txFrom.transactionIndex && it.interTransactionIndex < fromTransaction.interTransactionIndex
                            }
                }
            }
            limit?.let {
                transactions = transactions.take(it)
            }

            Single.just(transactions)
        }
    }

    override fun getPendingTransactions(): List<Transaction> {
        return database.transactionDao.getPendingTransactions()
    }

    override fun save(transactions: List<Transaction>) {
        database.transactionDao.insert(transactions)
    }


    // ITokenBalanceStorage

    override fun getBalance(): BigInteger? {
        return database.tokenBalanceDao.getBalance()?.value
    }

    override fun save(balance: BigInteger) {
        database.tokenBalanceDao.insert(TokenBalance(balance))
    }

}