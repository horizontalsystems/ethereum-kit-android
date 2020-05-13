package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class TransactionManager(
        private val storage: ITransactionStorage,
        private val transactionsProvider: ITransactionsProvider
) : ITransactionManager {

    private var disposables = CompositeDisposable()

    override var syncState: SyncState = SyncState.NotSynced()
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateTransactionsSyncState(value)
            }
        }

    override val source: String
        get() = transactionsProvider.source

    override var listener: ITransactionManagerListener? = null

    private fun update(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions.filter { it.input.isEmpty() })
    }

    override fun refresh() {
        syncState = SyncState.Syncing()

        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight() ?: 0

        transactionsProvider.getTransactions(lastTransactionBlockHeight + 1)
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    update(transactions)
                    syncState = SyncState.Synced()
                }, {
                    syncState = SyncState.NotSynced()
                }).let {
                    disposables.add(it)
                }
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    override fun handle(transaction: EthereumTransaction) {
        update(listOf(transaction))
    }

}
