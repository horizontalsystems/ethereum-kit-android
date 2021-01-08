package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.INotSyncedTransactionPool
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerDelegate
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerStateStorage
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.horizontalsystems.ethereumkit.models.TransactionSyncerState
import io.reactivex.Flowable

class NotSyncedTransactionManager(
        private val pool: INotSyncedTransactionPool,
        private val storage: ITransactionSyncerStateStorage
) : ITransactionSyncerDelegate {

    override val notSyncedTransactionsSignal: Flowable<Unit>
        get() = pool.notSyncedTransactionsSignal

    override fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction> {
        return pool.getNotSyncedTransactions(limit)
    }

    override fun add(notSyncedTransactions: List<NotSyncedTransaction>) {
        pool.add(notSyncedTransactions)
    }

    override fun remove(notSyncedTransaction: NotSyncedTransaction) {
        pool.remove(notSyncedTransaction)
    }

    override fun update(notSyncedTransaction: NotSyncedTransaction) {
        pool.update(notSyncedTransaction)
    }

    override fun getTransactionSyncerState(id: String): TransactionSyncerState? {
        return storage.getTransactionSyncerState(id)
    }

    override fun update(transactionSyncerState: TransactionSyncerState) {
        storage.save(transactionSyncerState)
    }

}
