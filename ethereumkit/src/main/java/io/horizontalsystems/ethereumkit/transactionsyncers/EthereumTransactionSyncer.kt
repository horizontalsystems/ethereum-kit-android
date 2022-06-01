package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.storage.TransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.models.ProviderTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionSyncerState
import io.reactivex.Single

class EthereumTransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: TransactionSyncerStateStorage
): ITransactionSyncer {

    companion object {
        const val SyncerId = "ethereum-transaction-syncer"
    }

    override fun getTransactionsSingle(): Single<List<Transaction>> {
        val lastTransactionBlockNumber = storage.get(SyncerId)?.lastBlockNumber ?: 0

        return transactionProvider.getTransactions(lastTransactionBlockNumber + 1)
            .doOnSuccess { providerTransactions -> handle(providerTransactions) }
            .map { providerTransactions ->
            providerTransactions.map { transaction ->
                val isFailed = when {
                    transaction.txReceiptStatus != null -> {
                        transaction.txReceiptStatus != 1
                    }
                    transaction.isError != null -> {
                        transaction.isError != 0
                    }
                    transaction.gasUsed != null -> {
                        transaction.gasUsed == transaction.gasLimit
                    }
                    else -> {
                        false
                    }
                }

                Transaction(
                    hash = transaction.hash,
                    timestamp = transaction.timestamp,
                    isFailed = isFailed,
                    blockNumber = transaction.blockNumber,
                    transactionIndex = transaction.transactionIndex,
                    from = transaction.from,
                    to = transaction.to,
                    value = transaction.value,
                    input = transaction.input,
                    nonce = transaction.nonce,
                    gasPrice = transaction.gasPrice,
                    gasUsed = transaction.gasUsed
                )
            }
        }
    }

    private fun handle(transactions: List<ProviderTransaction>) {
        val maxBlockNumber = transactions.maxOfOrNull { it.blockNumber } ?: return
        val syncerState = TransactionSyncerState(SyncerId, maxBlockNumber)

        storage.save(syncerState)
    }

}
