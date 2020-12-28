package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger
import java.util.logging.Logger

interface ITransactionSyncer {
    val id: String
    val syncState: EthereumKit.SyncState
    val stateAsync: Flowable<EthereumKit.SyncState>

    fun sync()

    fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {}
    fun onUpdateNonce(nonce: Long) {}
    fun onUpdateBalance(balance: BigInteger) {}
}

class TransactionSyncManager : ITransactionSyncerListener {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()
    private val syncerStateDisposables = hashMapOf<String, Disposable>()

    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()
    private val transactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val syncers: MutableList<ITransactionSyncer> = mutableListOf()

    private lateinit var ethereumKit: EthereumKit

    var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val syncStateAsync: Flowable<EthereumKit.SyncState> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    val transactionsAsync: Flowable<List<FullTransaction>> = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun set(ethereumKit: EthereumKit) {
        this.ethereumKit = ethereumKit

        subscribe(ethereumKit)
    }

    fun add(syncer: ITransactionSyncer) {
        syncers.add(syncer)

        syncer.stateAsync
                .subscribeOn(Schedulers.io())
                .subscribe { syncState() }
                .let { syncerStateDisposables[syncer.id] = it }

        syncState()
    }

    fun removeSyncer(id: String) {
        syncerStateDisposables.remove(id)?.dispose()

        syncers.removeIf { it.id == id }
    }

    override fun onTransactionsSynced(transactions: List<FullTransaction>) {
        transactionsSubject.onNext(transactions)
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
        logger.info(" ---> onEthKitSyncState: $state, syncers: ${syncers.size}")

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
        val notSyncedSyncerState = syncers.firstOrNull { it.syncState is EthereumKit.SyncState.NotSynced }?.syncState as? EthereumKit.SyncState.NotSynced
        syncState = when {
            notSyncedSyncerState != null -> EthereumKit.SyncState.NotSynced(notSyncedSyncerState.error)
            syncers.any { it.syncState is EthereumKit.SyncState.Syncing } -> EthereumKit.SyncState.Syncing()
            else -> EthereumKit.SyncState.Synced()
        }
    }


}
