package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.models.TransactionReceipt
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.logging.Logger

class OutgoingPendingTransactionSyncer(
        private val blockchain: IBlockchain,
        private val storage: Storage
) : ITransactionSyncer {

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    override val id: String = "outgoing_pending_transaction_syncer"

    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    var listener: ITransactionSyncerListener? = null

    override var state: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    override val stateAsync: Flowable<EthereumKit.SyncState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        doSync().subscribeOn(Schedulers.io())
                .subscribe({
                    state = EthereumKit.SyncState.Synced()
                }, {
                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun doSync(): Single<Unit> {
        val pendingTransaction = storage.getFirstPendingTransaction() ?: return Single.just(Unit)
        logger.info("---> doSync() pendingTransaction: $pendingTransaction")

        return blockchain.getTransactionReceipt(pendingTransaction.hash)
                .flatMap {
                    logger.info("---> sync() onFetched receipt: ${it.orElse(null)?.transactionHash}")

                    if (it.isPresent) {
                        val rpcReceipt = it.get()

                        storage.save(TransactionReceipt(rpcReceipt))
                        storage.save(rpcReceipt.logs)

                        listener?.onTransactionsSynced(storage.getTransactions(listOf(rpcReceipt.transactionHash)))

                        doSync()
                    } else {
                        state = EthereumKit.SyncState.Synced()
                        Single.just(Unit)
                    }
                }
    }

}
