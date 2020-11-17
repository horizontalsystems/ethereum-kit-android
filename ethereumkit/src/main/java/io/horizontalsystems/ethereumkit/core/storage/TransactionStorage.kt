package io.horizontalsystems.ethereumkit.core.storage

import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.reactivex.Single
import java.math.BigInteger

class TransactionStorage(private val database: TransactionDatabase) : ITransactionStorage {

    override fun getLastTransactionBlockHeight(): Long? {
        return database.transactionDao().getLastTransaction()?.blockNumber
    }

    override fun getLastInternalTransactionBlockHeight(): Long? {
        return database.transactionDao().getLastInternalTransaction()?.blockNumber
    }

    override fun saveTransactions(transactions: List<Transaction>) {
        database.transactionDao().insert(transactions)
    }

    override fun saveInternalTransactions(transactions: List<InternalTransaction>) {
        transactions.forEach {
            try {
                database.transactionDao().insertInternal(it)
            } catch (error: Throwable) {
                //ignore internal tx if no main tx exists
            }
        }
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionWithInternal>> {
        return database.transactionDao().getTransactions()
                .flatMap { transactionsList ->
                    var transactions = transactionsList.filter { it.internalTransactions.count() > 0 || it.transaction.value > BigInteger.ZERO }

                    fromHash?.let { fromHash ->
                        val txFrom = transactions.firstOrNull { it.transaction.hash.contentEquals(fromHash) }?.transaction
                        txFrom?.let {
                            transactions = transactions.filter {
                                it.transaction.timestamp < txFrom.timestamp ||
                                        (it.transaction.timestamp == txFrom.timestamp && (it.transaction.transactionIndex?.compareTo(txFrom.transactionIndex ?: 0) ?: 0) < 0)
                            }

                        }
                    }

                    limit?.let {
                        transactions = transactions.take(it)
                    }

                    Single.just(transactions)
                }
    }

}
