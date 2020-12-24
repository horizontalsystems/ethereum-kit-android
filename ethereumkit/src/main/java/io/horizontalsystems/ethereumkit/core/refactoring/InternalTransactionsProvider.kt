package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionsProvider
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import java.util.logging.Logger

class InternalTransactionsProvider(
        private val ethereumTransactionProvider: ITransactionsProvider,
        private val notSyncedTransactionPool: NotSyncedTransactionPool,
        val storage: Storage
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
        TODO("not implemented")
    }

}
