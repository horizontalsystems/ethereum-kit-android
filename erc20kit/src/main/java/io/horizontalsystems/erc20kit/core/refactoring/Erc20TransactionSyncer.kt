package io.horizontalsystems.erc20kit.core.refactoring

import io.horizontalsystems.erc20kit.core.EtherscanTransactionsProvider
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.refactoring.AbstractTransactionSyncer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class Erc20TransactionSyncer(
        override val id: String,
        private val address: Address,
        private val contractAddress: Address,
        private val etherscanTransactionsProvider: EtherscanTransactionsProvider
) : AbstractTransactionSyncer(id) {

    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val disposables = CompositeDisposable()

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onLastBlockBloomFilter(bloomFilter: BloomFilter) {
        if (bloomFilter.mayContainContractAddress(contractAddress)) {
            sync()
        }
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        state = EthereumKit.SyncState.Syncing()

        etherscanTransactionsProvider.getTokenTransactions(address, contractAddress, lastSyncBlockNumber + 1)
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

                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

}