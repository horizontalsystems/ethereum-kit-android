package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
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

class EthereumTransactionProvider(
        private val ethereumTransactionProvider: ITransactionsProvider,
        private val notSyncedTransactionPool: NotSyncedTransactionPool
) : ITransactionSyncer {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    private val disposables = CompositeDisposable()
    private val stateSubject = PublishSubject.create<EthereumKit.SyncState>()

    //TODO persist to db
    private var lastSyncBlockHeight: Long? = null

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

        // gets transaction starting from last tx's block height
        ethereumTransactionProvider
                .getTransactions(lastSyncBlockHeight ?: 0)
                .map { txList ->
                    lastSyncBlockHeight = txList.lastOrNull()?.blockNumber

                    txList.map { etherscanTransaction ->
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

                    logger.info("---> sync() onFetched notSyncedTransactions: ${notSyncedTransactions.size}")

                    notSyncedTransactionPool.add(notSyncedTransactions)
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
