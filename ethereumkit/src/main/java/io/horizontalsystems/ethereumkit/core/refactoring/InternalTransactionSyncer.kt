package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EtherscanTransactionsProvider
import io.horizontalsystems.ethereumkit.core.RetryOptions
import io.horizontalsystems.ethereumkit.core.retryWith
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

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onAccountState(accountState: AccountState) {
        sync(retry = true)
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

                        delegate.add(internalTransactions.map { NotSyncedTransaction(it.hash) })

                        internalTransactions.firstOrNull()?.blockNumber?.let {
                            lastSyncBlockNumber = it
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
