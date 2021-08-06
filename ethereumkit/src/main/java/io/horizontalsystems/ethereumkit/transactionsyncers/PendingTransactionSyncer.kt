package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.DroppedTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionReceipt
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class PendingTransactionSyncer(
        private val blockchain: IBlockchain,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("outgoing_pending_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    var listener: ITransactionSyncerListener? = null

    override fun onLastBlockNumber(blockNumber: Long) {
        sync()
    }

    private fun sync() {
        logger.info("---> sync() state: $state")

        if (state is EthereumKit.SyncState.Syncing) return

        doSync().subscribeOn(Schedulers.io())
                .subscribe({
                    state = EthereumKit.SyncState.Synced()
                }, {
                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun doSync(fromTransaction: Transaction? = null): Single<Unit> {
        val pendingTransactions = storage.getPendingTransactions(fromTransaction)
        val notReplacedPendingTransactions = replaceDuplicateTransactions(pendingTransactions)
        logger.info("---> doSync() pendingTransactions: ${pendingTransactions.joinToString(separator = ",") { it.hash.toHexString() }}")

        if (notReplacedPendingTransactions.isEmpty()) {
            return Single.just(Unit)
        }

        val singles: List<Single<Unit>> = notReplacedPendingTransactions.map { pendingTransaction ->
            blockchain.getTransactionReceipt(pendingTransaction.hash)
                    .flatMap { optionalReceipt ->
                        logger.info("---> doSync() onFetched receipt: ${optionalReceipt.orElse(null)?.transactionHash}")
                        syncTimestamp(pendingTransaction, optionalReceipt.orElse(null))
                    }
        }

        return Single.zip(singles) { }.flatMap {
            doSync(pendingTransactions.last())
        }
    }

    private fun replaceDuplicateTransactions(pendingTransactions: List<Transaction>): List<Transaction> {
        val notReplaced = mutableListOf<Transaction>()
        val droppedTransactions = mutableListOf<ByteArray>()

        pendingTransactions.forEach { transaction ->
            val duplicatedTransaction = storage.getInBlockTransaction(transaction.nonce)
            if (duplicatedTransaction != null) {
                storage.addDroppedTransaction(DroppedTransaction(transaction.hash, duplicatedTransaction.hash))
                droppedTransactions.add(transaction.hash)
            } else {
                notReplaced.add(transaction)
            }
        }

        if (droppedTransactions.isNotEmpty()) {
            listener?.onTransactionsSynced(storage.getFullTransactions(droppedTransactions))
        }

        return notReplaced
    }

    private fun syncTimestamp(transaction: Transaction, receipt: RpcTransactionReceipt?): Single<Unit> {
        if (receipt == null) {
            return Single.just(Unit)
        }

        return blockchain.getBlock(receipt.blockNumber)
                .doOnSuccess { optionalBlock ->
                    logger.info("---> sync() onFetched block: $optionalBlock")

                    handle(transaction, receipt, optionalBlock.orElse(null)?.timestamp)
                }
                .map { }
    }

    private fun handle(transaction: Transaction, receipt: RpcTransactionReceipt, timestamp: Long?) {
        if (timestamp == null) {
            return
        }

        transaction.timestamp = timestamp

        storage.save(transaction)
        storage.save(TransactionReceipt(receipt))
        storage.save(receipt.logs)

        listener?.onTransactionsSynced(storage.getFullTransactions(listOf(receipt.transactionHash)))
    }

}
