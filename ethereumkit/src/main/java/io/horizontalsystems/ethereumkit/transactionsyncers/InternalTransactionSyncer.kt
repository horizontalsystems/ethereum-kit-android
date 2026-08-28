package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderInternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import kotlinx.coroutines.CancellationException

class InternalTransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: ITransactionStorage
) : ITransactionSyncer {

    private fun handle(transactions: List<ProviderInternalTransaction>) {
        if (transactions.isEmpty()) return

        val internalTransactions = transactions.map { tx ->
            InternalTransaction(tx.hash, tx.blockNumber, tx.from, tx.to, tx.value)
        }

        storage.saveInternalTransactions(internalTransactions)
    }

    override suspend fun getTransactions(): Pair<List<Transaction>, Boolean> {
        val lastTransactionBlockNumber = storage.getLastInternalTransaction()?.blockNumber ?: 0
        val initial = lastTransactionBlockNumber == 0L

        return try {
            val providerInternalTransactions = transactionProvider.getInternalTransactions(lastTransactionBlockNumber + 1)
            handle(providerInternalTransactions)

            val array = providerInternalTransactions.map { transaction ->
                Transaction(
                        hash = transaction.hash,
                        timestamp = transaction.timestamp,
                        isFailed = false,
                        blockNumber = transaction.blockNumber,
                )
            }
            Pair(array, initial)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Pair(listOf(), initial)
        }
    }

}
