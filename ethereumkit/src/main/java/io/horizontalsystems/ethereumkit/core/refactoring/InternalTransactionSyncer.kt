package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EtherscanTransactionsProvider
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.horizontalsystems.ethereumkit.models.TransactionSyncerState
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.logging.Logger

class InternalTransactionSyncer(
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider,
        private val notSyncedTransactionPool: NotSyncedTransactionPool,
        val storage: IStorage
) : ITransactionSyncer {

    override val id: String = "internal_transaction_syncer"

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    private var lastSyncBlockNumber: Long = storage.getTransactionSyncerState(id)?.lastBlockNumber ?: 0
        set(value) {
            field = value
            storage.save(TransactionSyncerState(id, value))
        }

    override var state: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    override val stateAsync: Flowable<EthereumKit.SyncState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        etherscanTransactionsProvider.getInternalTransactions(lastSyncBlockNumber + 1)
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    storage.saveInternalTransactions(transactions)

                    logger.info("---> sync() onFetched: ${transactions.size}")

                    notSyncedTransactionPool.add(transactions.map { NotSyncedTransaction(it.hash) })

                    transactions.firstOrNull()?.blockNumber?.let {
                        lastSyncBlockNumber = it
                    }
                    state = EthereumKit.SyncState.Synced()
                }, {
                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onUpdateNonce(nonce: Long) {
        sync()
    }

    override fun onUpdateBalance(balance: BigInteger) {
        sync()
    }

}
