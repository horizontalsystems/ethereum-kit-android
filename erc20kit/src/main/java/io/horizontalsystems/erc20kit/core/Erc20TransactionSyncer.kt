package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.RetryOptions
import io.horizontalsystems.ethereumkit.core.retryWith
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.horizontalsystems.ethereumkit.transactionsyncers.AbstractTransactionSyncer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class Erc20TransactionSyncer(
        override val id: String,
        private val address: Address,
        private val contractAddress: Address,
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider
) : AbstractTransactionSyncer(id) {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val reSync = AtomicBoolean(false)

    override fun start() {
        sync()
    }

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {
        if (bloomFilter.mayContainContractAddress(contractAddress)) {
            sync(retry = true)
        }
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
        var getTransactionsSingle = etherscanTransactionsProvider.getTokenTransactions(address, contractAddress, lastSyncBlockNumber + 1)
        if (retry) {
            getTransactionsSingle = getTransactionsSingle.retryWith(RetryOptions { it.isEmpty() })
        }

        getTransactionsSingle
                .subscribeOn(Schedulers.io())
                .subscribe({ tokenTransactions ->
                    logger.info("---> sync() onFetched: ${tokenTransactions.size}")

                    if (tokenTransactions.isNotEmpty()) {
                        val latestBlockNumber = tokenTransactions.maxByOrNull { it.blockNumber ?: 0 }?.blockNumber
                        latestBlockNumber?.let {
                            lastSyncBlockNumber = it
                        }

                        val notSyncedTransactions = tokenTransactions.map { NotSyncedTransaction(it.hash) }
                        delegate.add(notSyncedTransactions)
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
