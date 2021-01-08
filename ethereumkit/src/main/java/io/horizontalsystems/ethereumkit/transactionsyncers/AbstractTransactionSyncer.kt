package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerDelegate
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.TransactionSyncerState
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject

abstract class AbstractTransactionSyncer(
        override val id: String
) : ITransactionSyncer {

    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    override var state: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        protected set(value) {
            field = value
            stateSubject.onNext(value)
        }
    override val stateAsync: Flowable<EthereumKit.SyncState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    protected lateinit var delegate: ITransactionSyncerDelegate

    protected var lastSyncBlockNumber: Long
        get() = delegate.getTransactionSyncerState(id)?.lastBlockNumber ?: 0
        set(value) {
            delegate.update(TransactionSyncerState(id, value))
        }

    override fun set(delegate: ITransactionSyncerDelegate) {
        this.delegate = delegate
    }

    override fun onEthereumKitSynced() {}

    override fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {}

    override fun onAccountState(accountState: AccountState) {}

    override fun onLastBlockNumber(blockNumber: Long) {}

}
