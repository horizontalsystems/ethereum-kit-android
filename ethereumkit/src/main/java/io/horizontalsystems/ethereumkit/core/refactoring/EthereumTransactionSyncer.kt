package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
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

class EthereumTransactionSyncer(
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider,
        private val notSyncedTransactionPool: NotSyncedTransactionPool,
        private val storage: IStorage
) : ITransactionSyncer {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    override val id: String = "ethereum_transaction_syncer"

    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    private var lastSyncBlockNumber: Long = storage.getTransactionSyncerState(id)?.lastBlockNumber ?: 0
        set(value) {
            field = value
            logger.info("---> set lastSyncBlockNumber: $value")
            storage.save(TransactionSyncerState(id, value))
        }

    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            field = value
            stateSubject.onNext(value)
        }

    override val stateAsync: Flowable<EthereumKit.SyncState>
        get() = stateSubject.toFlowable(BackpressureStrategy.BUFFER)

    override fun sync() {
        logger.info("---> sync() state: $syncState")

        if (syncState is EthereumKit.SyncState.Syncing) return

        syncState = EthereumKit.SyncState.Syncing()

        // gets transaction starting from last tx's block height
        etherscanTransactionsProvider
                .getTransactions(lastSyncBlockNumber + 1)
                .map { transactions ->
                    transactions.map { etherscanTransaction ->
                        NotSyncedTransaction(
                                hash = etherscanTransaction.hash,
                                transaction = RpcTransaction(
                                        hash = etherscanTransaction.hash,
                                        nonce = etherscanTransaction.nonce,
                                        blockHash = etherscanTransaction.blockHash,
                                        blockNumber = etherscanTransaction.blockNumber,
                                        transactionIndex = etherscanTransaction.transactionIndex,
                                        from = etherscanTransaction.from,
                                        to = etherscanTransaction.to,
                                        value = etherscanTransaction.value,
                                        gasPrice = etherscanTransaction.gasPrice,
                                        gasLimit = etherscanTransaction.gasLimit,
                                        input = etherscanTransaction.input
                                ),
                                timestamp = etherscanTransaction.timestamp
                        )
                    }
                }
                .subscribeOn(Schedulers.io())
                .subscribe({ notSyncedTransactions ->

                    logger.info("---> sync() onFetched: ${notSyncedTransactions.size}")

                    notSyncedTransactionPool.add(notSyncedTransactions)
                    notSyncedTransactions.firstOrNull()?.transaction?.blockNumber?.let {
                        lastSyncBlockNumber = it
                    }
                    syncState = EthereumKit.SyncState.Synced()
                }, {
                    syncState = EthereumKit.SyncState.NotSynced(it)
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
