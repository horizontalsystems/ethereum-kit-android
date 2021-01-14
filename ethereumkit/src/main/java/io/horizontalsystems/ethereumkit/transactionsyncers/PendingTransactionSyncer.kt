package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.core.ITransactionSyncerListener
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionReceipt
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.logging.Logger

class PendingTransactionSyncer(
        private val blockchain: IBlockchain,
        private val storage: ITransactionStorage
) : AbstractTransactionSyncer("outgoing_pending_transaction_syncer") {

    private val logger = Logger.getLogger(this.javaClass.simpleName)

    var listener: ITransactionSyncerListener? = null

    override fun onEthereumKitSynced() {
        sync()
    }

    override fun onUpdateAccountState(accountState: AccountState) {
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

    private fun doSync(): Single<Unit> {
        val pendingTransaction = storage.getFirstPendingTransaction() ?: return Single.just(Unit)
        logger.info("---> doSync() pendingTransaction: $pendingTransaction")

        return blockchain.getTransactionReceipt(pendingTransaction.hash)
                .flatMap { optionalReceipt ->
                    logger.info("---> sync() onFetched receipt: ${optionalReceipt.orElse(null)?.transactionHash}")

                    syncTimestamp(pendingTransaction, optionalReceipt.orElse(null))
                }
    }

    private fun syncTimestamp(transaction: Transaction, receipt: RpcTransactionReceipt?): Single<Unit> {
        if (receipt == null) {
            return Single.just(Unit)
        }

        return blockchain.getBlock(receipt.blockNumber)
                .flatMap { optionalBlock ->
                    logger.info("---> sync() onFetched block: $optionalBlock")

                    handle(transaction, receipt, optionalBlock.orElse(null)?.timestamp)
                }
    }

    private fun handle(transaction: Transaction, receipt: RpcTransactionReceipt, timestamp: Long?): Single<Unit> {
        if (timestamp == null) {
            return Single.just(Unit)
        }

        transaction.timestamp = timestamp

        storage.save(transaction)
        storage.save(TransactionReceipt(receipt))
        storage.save(receipt.logs)

        listener?.onTransactionsSynced(storage.getFullTransactions(listOf(receipt.transactionHash)))

        return doSync()
    }

}
