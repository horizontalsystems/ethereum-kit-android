package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.DroppedTransaction
import io.horizontalsystems.ethereumkit.models.NotSyncedTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionReceipt
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.logging.Logger

class TransactionSyncer(
        private val blockchain: IBlockchain,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("full_transaction_syncer") {
    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val txSyncBatchSize = 10

    var listener: ITransactionSyncerListener? = null

    override fun onEthereumKitSynced() {
        sync()
    }

    private fun sync() {
        logger.info("---> sync()  $state")

        if (state is EthereumKit.SyncState.Syncing) return

        doSync()
    }

    private fun doSync() {
        val notSyncedTransactions = delegate.getNotSyncedTransactions(txSyncBatchSize)
        logger.info("---> notSyncedTransactions: ${notSyncedTransactions.size} ")

        if (notSyncedTransactions.isEmpty()) {
            state = EthereumKit.SyncState.Synced()
            return
        } else {
            state = EthereumKit.SyncState.Syncing()
        }

        Single
                .zip(notSyncedTransactions.map { syncSingle(it) }) { singleResults ->
                    singleResults.mapNotNull { it as? Optional<ByteArray> }
                }
                .subscribeOn(Schedulers.io())
                .subscribe({ syncedTxHashes ->
                    logger.info("---> synced batch: ${syncedTxHashes.joinToString(separator = ",") { it.orElse(null)?.toHexString() ?: "null" }}")

                    val txHashes = syncedTxHashes.mapNotNull { it.orElse(null) }
                    listener?.onTransactionsSynced(storage.getFullTransactions(txHashes))

                    doSync()
                }, {
                    state = EthereumKit.SyncState.NotSynced(it)
                })
                .let { disposables.add(it) }
    }

    private fun syncSingle(notSyncedTransaction: NotSyncedTransaction): Single<Optional<ByteArray>> {
        return syncTransactionSingle(notSyncedTransaction)
                .flatMap { rpcTransaction ->
                    if (rpcTransaction.isPresent) {
                        syncReceiptSingle(rpcTransaction.get())
                    } else {
                        Single.just(Optional.empty())
                    }
                }
                .flatMap { rpcTransactionAndReceipt ->
                    if (rpcTransactionAndReceipt.isPresent) {
                        syncTimestampSingle(rpcTransactionAndReceipt.get(), notSyncedTransaction)
                    } else {
                        Single.just(Optional.empty())
                    }
                }
                .map { rpcTransactionAndTimestamp ->
                    if (rpcTransactionAndTimestamp.isPresent) {
                        val (rpcTransaction, timestamp) = rpcTransactionAndTimestamp.get()
                        finalizeSync(notSyncedTransaction, rpcTransaction, timestamp)
                        Optional.ofNullable(rpcTransaction.hash)
                    } else {
                        Optional.empty()
                    }
                }
    }

    private fun finalizeSync(notSyncedTransaction: NotSyncedTransaction, transaction: RpcTransaction, timestamp: Long) {
        val transactionEntity = Transaction(
                hash = transaction.hash,
                nonce = transaction.nonce,
                from = transaction.from,
                to = transaction.to,
                value = transaction.value,
                gasPrice = transaction.gasPrice,
                maxFeePerGas = null,
                maxPriorityFeePerGas = null,
                gasLimit = transaction.gasLimit,
                input = transaction.input,
                timestamp = timestamp
        )
        storage.getPendingTransactionList(transaction.nonce).forEach { duplicateTransaction ->
            storage.addDroppedTransaction(DroppedTransaction(hash = duplicateTransaction.hash, replacedWith = transaction.hash))

            listener?.onTransactionsSynced(storage.getFullTransactions(listOf(duplicateTransaction.hash)))
        }
        storage.save(transactionEntity)

        delegate.remove(notSyncedTransaction)
    }

    private fun syncTransactionSingle(notSyncedTransaction: NotSyncedTransaction): Single<Optional<RpcTransaction>> {
        return if (notSyncedTransaction.transaction != null) {
            Single.just(Optional.of(notSyncedTransaction.transaction!!))
        } else {
            blockchain.getTransaction(notSyncedTransaction.hash)
                    .doOnSuccess {
                        it.orElse(null)?.let { transaction ->
                            notSyncedTransaction.transaction = transaction
                            delegate.update(notSyncedTransaction)
                        }
                    }
        }
    }

    private fun syncReceiptSingle(transaction: RpcTransaction): Single<Optional<Pair<RpcTransaction, RpcTransactionReceipt?>>> {
        if (transaction.blockNumber == null) {
            return Single.just(Optional.of(Pair(transaction, null)))
        }

        val transactionReceipt = storage.getTransactionReceipt(transaction.hash)
        if (transactionReceipt != null) {
            return Single.just(Optional.of(Pair(transaction, RpcTransactionReceipt(transactionReceipt, emptyList()))))
        } else {
            return blockchain.getTransactionReceipt(transaction.hash)
                    .doOnSuccess {
                        if (it.isPresent) {
                            val rpcReceipt = it.get()
                            storage.save(TransactionReceipt(rpcReceipt))
                            storage.save(rpcReceipt.logs)
                        }
                    }
                    .map {
                        if (!it.isPresent) {
                            Optional.empty()
                        } else {
                            Optional.of(Pair(transaction, it.get()))
                        }
                    }
        }
    }

    private fun syncTimestampSingle(transactionAndReceipt: Pair<RpcTransaction, RpcTransactionReceipt?>, notSyncedTransaction: NotSyncedTransaction): Single<Optional<Pair<RpcTransaction, Long>>> {
        val (transaction, transactionReceipt) = transactionAndReceipt

        if (transactionReceipt == null) {
            return Single.just(Optional.ofNullable(Pair(transaction, System.currentTimeMillis() / 1000)))
        }

        if (notSyncedTransaction.timestamp != null) {
            return Single.just(Optional.ofNullable(Pair(transaction, notSyncedTransaction.timestamp)))
        }

        return blockchain.getBlock(transactionReceipt.blockNumber)
                .map {
                    if (!it.isPresent) {
                        Optional.empty()
                    } else {
                        Optional.of(Pair(transaction, it.get().timestamp))
                    }
                }
    }

    override fun set(delegate: ITransactionSyncerDelegate) {
        super.set(delegate)

        //subscribe to txHashPool and sync when new hashes received
        delegate.notSyncedTransactionsSignal
                .subscribeOn(Schedulers.io())
                .subscribe {
                    logger.info("---> notSyncedTransactionsSignal")
                    sync()
                }
                .let { disposables.add(it) }
    }

}
