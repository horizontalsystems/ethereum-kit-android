package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionsProvider
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.logging.Logger

class InternalTransactionsProvider(
        private val ethereumTransactionProvider: ITransactionsProvider,
        private val notSyncedTransactionPool: NotSyncedTransactionPool,
        val storage: IStorage
) : ITransactionSyncer {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    override var state: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    override val stateFlowable: Flowable<EthereumKit.SyncState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        val lastSyncBlockNumber = storage.getLastInternalTransactionBlockHeight() ?: 0

        ethereumTransactionProvider.getInternalTransactions(lastSyncBlockNumber)
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    storage.saveInternalTransactions(transactions)
                    notSyncedTransactionPool.add(transactions.map { NotSyncedTransaction(it.hash) })

                    state = EthereumKit.SyncState.Synced()
                }, {
                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    override fun onUpdateNonce(nonce: Long) {
        sync()
    }

    override fun onUpdateBalance(balance: BigInteger) {
        sync()
    }

}
