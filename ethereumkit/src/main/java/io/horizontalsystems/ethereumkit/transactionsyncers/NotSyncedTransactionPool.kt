package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.INotSyncedTransactionPool
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import java.util.logging.Logger

class NotSyncedTransactionPool(
        private val storage: ITransactionStorage
) : INotSyncedTransactionPool {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val notSyncedTransactionsSubject = PublishSubject.create<Unit>()

    override val notSyncedTransactionsSignal: Flowable<Unit>
        get() = notSyncedTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun add(notSyncedTransactions: List<NotSyncedTransaction>) {
        val syncedTransactionHashes = storage.getTransactionHashes()

        val newTransactions = notSyncedTransactions.filter { notSyncedTransaction ->
            syncedTransactionHashes.none { notSyncedTransaction.hash.contentEquals(it) }
        }
        storage.addNotSyncedTransactions(newTransactions)

        logger.info("---> add notSyncedTransactions: ${newTransactions.size}")
        if (newTransactions.isNotEmpty()) {
            notSyncedTransactionsSubject.onNext(Unit)

            logger.info("---> notSyncedTransactionsSubject.onNext")
        }
    }

    override fun remove(notSyncedTransaction: NotSyncedTransaction) {
        storage.remove(notSyncedTransaction)
    }

    override fun update(notSyncedTransaction: NotSyncedTransaction) {
        storage.update(notSyncedTransaction)
    }

    override fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction> {
        return storage.getNotSyncedTransactions(limit)
    }

}
