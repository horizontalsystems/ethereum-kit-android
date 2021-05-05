package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerListener
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

class TransactionSyncManager(
        private val notSyncedTransactionManager: NotSyncedTransactionManager
) : ITransactionSyncerListener {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()
    private val syncerStateDisposables = hashMapOf<String, Disposable>()

    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()
    private val transactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val syncers = CopyOnWriteArrayList<ITransactionSyncer>()

    private lateinit var ethereumKit: EthereumKit
    private var accountState: AccountState? = null

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
        if (syncers.any { it.id == syncer.id }) return

        syncer.set(delegate = notSyncedTransactionManager)

        syncers.add(syncer)

        syncer.stateAsync
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { syncState() }
                .let { syncerStateDisposables[syncer.id] = it }

        syncer.start()
    }

    fun removeSyncer(id: String) {
        syncers.firstOrNull { it.id == id }?.let { syncer ->
            stopSyncer(syncer)
            syncers.remove(syncer)
        }
    }

    fun stop() {
        syncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())

        syncers.forEach {
            stopSyncer(it)
        }
        syncers.clear()
    }

    override fun onTransactionsSynced(transactions: List<FullTransaction>) {
        transactionsSubject.onNext(transactions)
    }

    private fun stopSyncer(syncer: ITransactionSyncer) {
        syncerStateDisposables.remove(syncer.id)?.dispose()
        syncer.stop()
    }

    private fun subscribe(ethereumKit: EthereumKit) {
        accountState = ethereumKit.accountState

        ethereumKit.accountStateFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    logger.info(" ---> accountStateFlowable: $it, syncers: ${syncers.size}")
                    onAccountState(it)
                }
                .let { disposables.add(it) }

        ethereumKit.lastBlockHeightFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    logger.info(" ---> lastBlockHeightFlowable: $it, syncers: ${syncers.size}")
                    onLastBlockNumber(it)
                }
                .let { disposables.add(it) }

        ethereumKit.syncStateFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    logger.info(" ---> syncStateFlowable: $it, syncers: ${syncers.size}")
                    onEthereumKitSyncState(it)
                }
                .let { disposables.add(it) }
    }

    private fun onEthereumKitSyncState(state: EthereumKit.SyncState) {
        logger.info(" ---> onEthKitSyncState: $state, syncers: ${syncers.size}")

        if (state is EthereumKit.SyncState.Synced) { //?? resync on network reconnection
            performOnSyncers { syncer -> syncer.onEthereumKitSynced() }
        }
    }

    private fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {
        performOnSyncers { syncer -> syncer.onLastBlockBloomFilter(bloomFilter) }
    }

    private fun onAccountState(accountState: AccountState) {
        if (this.accountState != null) {
            performOnSyncers { syncer -> syncer.onUpdateAccountState(accountState) }
        }
        this.accountState = accountState
    }

    private fun onLastBlockNumber(blockNumber: Long) {
        performOnSyncers { syncer -> syncer.onLastBlockNumber(blockNumber) }
    }

    private fun performOnSyncers(action: (ITransactionSyncer) -> Unit) {
        syncers.forEach { action(it) }
    }

    @Synchronized
    private fun syncState() {
        when (syncState) {
            is EthereumKit.SyncState.Synced -> {
                val notSyncedSyncerState = syncers.find { it.state is EthereumKit.SyncState.NotSynced }?.state
                if (notSyncedSyncerState != null) {
                    syncState = notSyncedSyncerState
                }
            }
            is EthereumKit.SyncState.Syncing, is EthereumKit.SyncState.NotSynced -> {
                syncState = syncers.find { it.state is EthereumKit.SyncState.NotSynced }?.state
                        ?: syncers.find { it.state is EthereumKit.SyncState.Syncing }?.state
                                ?: EthereumKit.SyncState.Synced()
            }
        }
    }

}
