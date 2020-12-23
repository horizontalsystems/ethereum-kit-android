package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

interface ITransactionSyncer {
    val state: EthereumKit.SyncState
    val stateFlowable: Flowable<EthereumKit.SyncState>

    fun sync()

    fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {}
    fun onUpdateNonce(nonce: Long) {}
    fun onUpdateBalance(balance: BigInteger) {}
}

class TransactionSyncManager {
    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()
    private val transactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val syncers: MutableList<ITransactionSyncer> = mutableListOf()

    private lateinit var ethereumKit: EthereumKit

    var state: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val stateFlowable: Flowable<EthereumKit.SyncState> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsFlowable = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)


    fun set(ethereumKit: EthereumKit) {
        this.ethereumKit = ethereumKit

        subscribe(ethereumKit)
    }

    private fun subscribe(ethereumKit: EthereumKit) {
        ethereumKit.balanceFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateBalance(it)
                }
                .let { disposables.add(it) }

        ethereumKit.nonceFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onUpdateNonce(it)
                }
                .let { disposables.add(it) }

        ethereumKit.lastBlockBloomFilterFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onLastBlockBloomFilter(it)
                }
                .let { disposables.add(it) }

        ethereumKit.syncStateFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onEthereumKitSyncState(it)
                }
                .let { disposables.add(it) }
    }

    private fun onEthereumKitSyncState(state: EthereumKit.SyncState) {
        if (state is EthereumKit.SyncState.Synced) { //?? resync on network reconnection
            performOnSyncers { syncer -> syncer.sync() }
        }
    }

    private fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {
        performOnSyncers { syncer -> syncer.onLastBlockBloomFilter(bloomFilter) }
    }

    private fun onUpdateNonce(nonce: Long) {
        performOnSyncers { syncer -> syncer.onUpdateNonce(nonce) }
    }

    private fun onUpdateBalance(balance: BigInteger) {
        performOnSyncers { syncer -> syncer.onUpdateBalance(balance) }
    }

    private fun performOnSyncers(action: (ITransactionSyncer) -> Unit) {
        syncers.forEach { action(it) }
    }

    private fun syncState() {
        val notSyncedSyncerState = syncers.firstOrNull { it.state is EthereumKit.SyncState.NotSynced }?.state as? EthereumKit.SyncState.NotSynced
        state = when {
            notSyncedSyncerState != null -> EthereumKit.SyncState.NotSynced(notSyncedSyncerState.error)
            syncers.any { it.state is EthereumKit.SyncState.Syncing } -> EthereumKit.SyncState.Syncing()
            else -> EthereumKit.SyncState.Synced()
        }
    }

    fun add(syncer: ITransactionSyncer) {
        syncers.add(syncer)

        syncer.stateFlowable
                .subscribeOn(Schedulers.io())
                .subscribe { syncState() }
                .let { disposables.add(it) }
        syncState()
    }

}
