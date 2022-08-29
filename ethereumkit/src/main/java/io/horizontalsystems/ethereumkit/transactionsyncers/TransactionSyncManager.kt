package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.TransactionManager
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Logger

class TransactionSyncManager(
        private val transactionManager: TransactionManager
) {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()

    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()
    private val syncers = CopyOnWriteArrayList<ITransactionSyncer>()

    var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }
    val syncStateAsync: Flowable<EthereumKit.SyncState> = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun add(syncer: ITransactionSyncer) {
        syncers.add(syncer)
    }

    fun sync() {
        if (syncState is EthereumKit.SyncState.Syncing) return

        syncState = EthereumKit.SyncState.Syncing()

        Single.zip(syncers.map {
            it.getTransactionsSingle()
        }) { array ->
            array.map { it as Pair<List<Transaction>, Boolean> }
                    .reduce { acc, list ->
                        Pair(acc.first + list.first, acc.second && list.second)
                    }
        }
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    handle(transactions)
                    syncState = EthereumKit.SyncState.Synced()
                }, {
                    syncState = EthereumKit.SyncState.NotSynced(it)
                    logger.warning("sync ERROR = ${it.message}")
                }).let {
                    disposables.add(it)
                }
    }

    private fun merge(tx1: Transaction, tx2: Transaction) =
            Transaction(
                    tx1.hash,
                    tx1.timestamp,
                    tx1.isFailed,
                    tx1.blockNumber ?: tx2.blockNumber,
                    tx1.transactionIndex ?: tx2.transactionIndex,
                    tx1.from ?: tx2.from,
                    tx1.to ?: tx2.to,
                    tx1.value ?: tx2.value,
                    tx1.input ?: tx2.input,
                    tx1.nonce ?: tx2.nonce,
                    tx1.gasPrice ?: tx2.gasPrice,
                    tx1.maxFeePerGas ?: tx2.maxFeePerGas,
                    tx1.maxPriorityFeePerGas ?: tx2.maxPriorityFeePerGas,
                    tx1.gasLimit ?: tx2.gasLimit,
                    tx1.gasUsed ?: tx2.gasUsed
            )

    private fun handle(result: Pair<List<Transaction>, Boolean>) {
        val transactions = result.first
        val initial = result.second

        val map: MutableMap<String, Transaction> = mutableMapOf()

        for (transaction in transactions) {
            val tx = map[transaction.hashString]

            if (tx == null) {
                map[transaction.hashString] = transaction
            } else {
                map[transaction.hashString] = merge(transaction, tx)
            }
        }

        transactionManager.handle(map.values.toList(), initial)
    }

}
