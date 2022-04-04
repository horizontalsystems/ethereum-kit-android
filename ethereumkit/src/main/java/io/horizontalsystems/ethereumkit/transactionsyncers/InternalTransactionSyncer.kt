package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderInternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction

class InternalTransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: ITransactionStorage
): ITransactionSyncer {

    private fun handle(transactions: List<ProviderInternalTransaction>) {
        if (transactions.isEmpty()) return

        val internalTransactions = transactions.map { tx ->
            InternalTransaction(tx.hash, tx.from, tx.to, tx.value)
        }

        storage.saveInternalTransactions(internalTransactions)
    }

    override fun getTransactionsSingle(lastTransactionBlockNumber: Long) =
        transactionProvider.getInternalTransactions(lastTransactionBlockNumber + 1)
            .doOnSuccess { providerInternalTransactions -> handle(providerInternalTransactions) }
            .map { providerInternalTransactions ->
            providerInternalTransactions.map { transaction ->
                Transaction(
                    hash = transaction.hash,
                    timestamp = transaction.timestamp,
                    isFailed = false,
                    blockNumber = transaction.blockNumber,
                )
            }
        }

}
