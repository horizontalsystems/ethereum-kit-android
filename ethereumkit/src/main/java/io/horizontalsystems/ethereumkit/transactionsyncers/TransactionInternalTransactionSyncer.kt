package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.NotSyncedInternalTransaction
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class TransactionInternalTransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("transaction_internal_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val watchers = mutableListOf<ITransactionWatcher>()

    var listener: ITransactionSyncerListener? = null

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    fun add(watcher: ITransactionWatcher) {
        watchers.add(watcher)
    }

    fun sync(transactions: List<FullTransaction>) {
        val confirmedTransactions = transactions.filter { it.receiptWithLogs != null }

        for (transaction in confirmedTransactions) {
            if (watchers.any { watcher -> watcher.needInternalTransactions(transaction) }) {
                storage.add(NotSyncedInternalTransaction(transaction.transaction.hash, 0))
                sync()
            }
        }
    }

    private fun sync() {
        if (state is EthereumKit.SyncState.Syncing) return

        doSync()
    }

    private fun doSync() {
        logger.info("---> sync() state: $state")

        val notSyncedInternalTransaction = storage.getNotSyncedInternalTransactions() ?: run {
            state = EthereumKit.SyncState.Synced()
            return
        }

        state = EthereumKit.SyncState.Syncing()

        transactionProvider.getInternalTransactionsAsync(notSyncedInternalTransaction.hash)
                .subscribeOn(Schedulers.io())
                .subscribe({ internalTransactions ->
                    handle(notSyncedInternalTransaction, internalTransactions)

                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun handle(notSyncedInternalTransaction: NotSyncedInternalTransaction, internalTransactions: List<InternalTransaction>) {
        logger.info("---> sync() onFetched: ${internalTransactions.size}")

        if (internalTransactions.isEmpty()) {
            if (notSyncedInternalTransaction.retryCount < 10) {
                notSyncedInternalTransaction.retryCount++
                storage.add(notSyncedInternalTransaction)
            } else {
                storage.remove(notSyncedInternalTransaction)
            }

            doSync()
        } else {
            storage.saveInternalTransactions(internalTransactions)
            storage.remove(notSyncedInternalTransaction)

            val fullTransactions = storage.getFullTransactions(listOf(notSyncedInternalTransaction.hash))
            listener?.onTransactionsSynced(fullTransactions)

            doSync()
        }
    }

}
