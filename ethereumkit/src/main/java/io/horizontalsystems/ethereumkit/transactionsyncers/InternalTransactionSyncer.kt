package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class InternalTransactionSyncer(
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("internal_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val disposables = CompositeDisposable()
    private val reSync = AtomicBoolean(false)

    var listener: ITransactionSyncerListener? = null

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onUpdateAccountState(accountState: AccountState) {
        sync(retry = true)
    }

    override fun stop() {
        disposables.clear()
    }

    private fun sync(retry: Boolean = false) {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) {
            if (retry) {
                reSync.set(true)
            }
            return
        }

        state = EthereumKit.SyncState.Syncing()
        doSync(retry)
    }

    private fun doSync(retry: Boolean) {
        var getTransactionsSingle = etherscanTransactionsProvider.getInternalTransactions(lastSyncBlockNumber + 1)
        if (retry) {
            getTransactionsSingle = getTransactionsSingle.retryWith(RetryOptions { it.isEmpty() })
        }

        getTransactionsSingle
                .subscribeOn(Schedulers.io())
                .subscribe({ internalTransactions ->
                    logger.info("---> sync() onFetched: ${internalTransactions.size}")

                    if (internalTransactions.isNotEmpty()) {
                        storage.saveInternalTransactions(internalTransactions)

                        internalTransactions.firstOrNull()?.blockNumber?.let {
                            lastSyncBlockNumber = it
                        }

                        val notSyncedTransactions = mutableListOf<NotSyncedTransaction>()
                        val syncedTransactions = mutableListOf<FullTransaction>()

                        internalTransactions.forEach { internalTransaction ->
                            val fullTransaction = storage.getFullTransaction(internalTransaction.hash)
                            if (fullTransaction != null) {
                                syncedTransactions.add(fullTransaction)
                            } else {
                                notSyncedTransactions.add(NotSyncedTransaction(internalTransaction.hash))
                            }
                        }

                        if (notSyncedTransactions.isNotEmpty()) {
                            delegate.add(notSyncedTransactions)
                        }

                        if (syncedTransactions.isNotEmpty()) {
                            listener?.onTransactionsSynced(syncedTransactions)
                        }
                    }

                    if (reSync.compareAndSet(true, false)) {
                        doSync(retry = true)
                    } else {
                        state = EthereumKit.SyncState.Synced()
                    }
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

}
