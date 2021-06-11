package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EtherscanTransactionsProvider
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerListener
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.NotSyncedInternalTransaction
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class TransactionInternalTransactionSyncer(
        private val provider: EtherscanTransactionsProvider,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("transaction_internal_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    var listener: ITransactionSyncerListener? = null

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    fun add(transactionHash: ByteArray) {
        storage.add(NotSyncedInternalTransaction(transactionHash))
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        val notSyncedInternalTransaction = storage.getNotSyncedInternalTransactions() ?: run {
            state = EthereumKit.SyncState.Synced()
            return
        }

        state = EthereumKit.SyncState.Syncing()

        provider.getInternalTransactions(notSyncedInternalTransaction)
                .subscribeOn(Schedulers.io())
                .subscribe({ internalTransactions ->
                    handle(notSyncedInternalTransaction, internalTransactions)

                    state = EthereumKit.SyncState.Synced()
                }, {
                    logger.info("---> sync() onError: ${it.message}")

                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun handle(notSyncedInternalTransaction: NotSyncedInternalTransaction, internalTransactions: List<InternalTransaction>) {
        logger.info("---> sync() onFetched: ${internalTransactions.size}")

        storage.saveInternalTransactions(internalTransactions)
        storage.remove(notSyncedInternalTransaction)

        val fullTransactions = storage.getFullTransactions(listOf(notSyncedInternalTransaction.hash))
        listener?.onTransactionsSynced(fullTransactions)

        sync()
    }

}
