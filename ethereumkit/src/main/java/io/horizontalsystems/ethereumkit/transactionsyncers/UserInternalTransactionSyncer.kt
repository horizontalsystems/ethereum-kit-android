package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerListener
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class UserInternalTransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("user_internal_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    var listener: ITransactionSyncerListener? = null

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        transactionProvider.getInternalTransactions(lastSyncBlockNumber + 1)
                .subscribeOn(Schedulers.io())
                .subscribe({ internalTransactions ->
                    handle(internalTransactions)

                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun handle(internalTransactions: List<InternalTransaction>) {
        logger.info("---> sync() onFetched: ${internalTransactions.size}")

        if (internalTransactions.isNotEmpty()) {
            storage.saveInternalTransactions(internalTransactions)

            internalTransactions.firstOrNull()?.blockNumber?.let {
                lastSyncBlockNumber = it
            }

            val notSyncedTransactions = mutableListOf<NotSyncedTransaction>()
            val syncedTransactions = mutableListOf<FullTransaction>()

            internalTransactions.forEach { internalTransaction ->
                val fullTransaction = storage.getFullTransaction(internalTransaction.hash)
                if (fullTransaction != null) {
                    syncedTransactions.add(fullTransaction)
                } else {
                    notSyncedTransactions.add(NotSyncedTransaction(internalTransaction.hash))
                }
            }

            if (notSyncedTransactions.isNotEmpty()) {
                delegate.add(notSyncedTransactions)
            }

            if (syncedTransactions.isNotEmpty()) {
                listener?.onTransactionsSynced(syncedTransactions)
            }
        }
    }

}
