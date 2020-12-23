package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

class NotSyncedTransactionPool(
        private val storage: IStorage
) {
    private val notSyncedTransactionsSubject = PublishSubject.create<Unit>()

    val notSyncedTransactionsSignal: Flowable<Unit>
        get() = notSyncedTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun add(notSyncedTransactions: List<NotSyncedTransaction>) {
        val syncedTransactionHashes = storage.getHashesFromTransactions()

        val newTransactions = notSyncedTransactions.filter { syncedTransactionHashes.contains(it.hash) }
        storage.addNotSyncedTransactions(newTransactions)

        notSyncedTransactionsSubject.onNext(Unit)
    }

    fun remove(notSyncedTransaction: NotSyncedTransaction) {
        storage.remove(notSyncedTransaction)
    }

    fun update(notSyncedTransaction: NotSyncedTransaction) {
        storage.update(notSyncedTransaction)
    }

    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction> {
        return storage.getNotSyncedTransactions(limit)
    }

}
