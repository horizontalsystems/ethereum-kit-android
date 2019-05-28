package io.horizontalsystems.ethereumkit.core.storage

import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single

class TransactionStorage(private val database: TransactionDatabase) : ITransactionStorage {

    override fun getLastTransactionBlockHeight(): Long? {
        return database.transactionDao().getLastTransaction()?.blockNumber
    }

    override fun saveTransactions(transactions: List<EthereumTransaction>) {
        database.transactionDao().insert(transactions)
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>> {
        val querySingle =
                if (contractAddress == null)
                    database.transactionDao().getTransactions()
                else
                    database.transactionDao().getErc20Transactions(contractAddress)

        return querySingle
                .flatMap { transactionsList ->
                    var transactions = transactionsList

                    fromHash?.let { fromHash ->
                        val tx = transactions.firstOrNull { it.hash.contentEquals(fromHash) }
                        tx?.timestamp?.let { txTimeStamp ->
                            transactions = transactions.filter { it.timestamp < txTimeStamp }
                        }
                    }

                    limit?.let {
                        transactions = transactions.take(it)
                    }

                    Single.just(transactions)
                }
    }

}
