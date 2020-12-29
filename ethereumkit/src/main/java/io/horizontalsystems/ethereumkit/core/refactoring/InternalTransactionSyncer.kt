package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EtherscanTransactionsProvider
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.util.logging.Logger

class InternalTransactionSyncer(
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider,
        private val storage: IStorage
) : AbstractTransactionSyncer("internal_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val disposables = CompositeDisposable()

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onUpdateNonce(nonce: Long) {
        sync()
    }

    override fun onUpdateBalance(balance: BigInteger) {
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        etherscanTransactionsProvider.getInternalTransactions(lastSyncBlockNumber + 1)
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
                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

}
