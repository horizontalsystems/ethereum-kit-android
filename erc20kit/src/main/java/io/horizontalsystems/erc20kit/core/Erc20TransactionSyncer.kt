package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.EtherscanTokenTransaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.horizontalsystems.ethereumkit.transactionsyncers.AbstractTransactionSyncer
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class Erc20TransactionSyncer(
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider
) : AbstractTransactionSyncer("erc20_transaction_syncer") {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    override fun start() {
        sync()
    }

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        etherscanTransactionsProvider.getTokenTransactions(lastSyncBlockNumber + 1)
                .subscribeOn(Schedulers.io())
                .subscribe({ tokenTransactions ->
                    logger.info("---> sync() onFetched: ${tokenTransactions.size}")

                    handle(tokenTransactions)
                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun handle(tokenTransactions: List<EtherscanTokenTransaction>) {
        if (tokenTransactions.isNotEmpty()) {
            val latestBlockNumber = tokenTransactions.maxByOrNull { it.blockNumber ?: 0 }?.blockNumber
            latestBlockNumber?.let {
                lastSyncBlockNumber = it
            }

            val notSyncedTransactions = tokenTransactions.map { NotSyncedTransaction(it.hash) }
            delegate.add(notSyncedTransactions)
        }
    }

}
