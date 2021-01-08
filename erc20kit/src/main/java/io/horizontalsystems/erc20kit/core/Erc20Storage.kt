package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.core.room.Erc20KitDatabase
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.TransactionCache
import io.reactivex.Single
import java.math.BigInteger

class Erc20Storage(
        database: Erc20KitDatabase
) : ITransactionStorage, ITokenBalanceStorage {

    private val transactionDao = database.transactionDao
    private val tokenBalanceDao = database.tokenBalanceDao

    // ITransactionStorage

    override fun getLastTransaction(): TransactionCache? {
        return transactionDao.getLastTransaction()
    }

    override fun save(transaction: TransactionCache) {
        transactionDao.insert(transaction)
    }

    override fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<TransactionCache>> {
        return transactionDao.getAllTransactions().flatMap { transactionsList ->
            var transactions = transactionsList

            fromTransaction?.let { fromTxKey ->
                transactions.firstOrNull { it.hash.contentEquals(fromTxKey.hash) && it.interTransactionIndex == fromTxKey.interTransactionIndex }?.let { txFrom ->
                    transactions = transactions
                            .filter {
                                it.timestamp < txFrom.timestamp || it.timestamp == txFrom.timestamp && (it.interTransactionIndex.compareTo(txFrom.interTransactionIndex)) < 0
                            }
                }
            }
            limit?.let {
                transactions = transactions.take(it)
            }

            Single.just(transactions)
        }
    }

    override fun getPendingTransactions(): List<TransactionCache> {
        return transactionDao.getPendingTransactions()
    }

    // ITokenBalanceStorage

    override fun getBalance(): BigInteger? {
        return tokenBalanceDao.getBalance()?.value
    }

    override fun save(balance: BigInteger) {
        tokenBalanceDao.insert(TokenBalance(balance))
    }

}