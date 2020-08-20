package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.TransactionWithInternal
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.functions.BiFunction
import java.math.BigInteger

class TransactionManager(
        private val storage: ITransactionStorage,
        private val transactionsProvider: ITransactionsProvider
) : ITransactionManager {

    private var disposables = CompositeDisposable()

    override var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateTransactionsSyncState(value)
            }
        }

    override val source: String
        get() = transactionsProvider.source

    override var listener: ITransactionManagerListener? = null

    private fun update(transactions: List<Transaction>, internalTransactions: List<InternalTransaction>, lastTransactionHash: ByteArray?) {
        storage.saveTransactions(transactions)
        storage.saveInternalTransactions(internalTransactions)

        val transactionsWithInternal = transactions.mapNotNull { transaction ->
            val internalTransactionList = internalTransactions.filter { it.hash.contentEquals(transaction.hash) }
            val transactionWithInternal = TransactionWithInternal(transaction, internalTransactionList)

            if (!isEmpty(transactionWithInternal)){
                transactionWithInternal
            } else {
                null
            }
        }

        listener?.onUpdateTransactions(transactionsWithInternal)
    }

    private fun isEmpty(transactionWithInternal: TransactionWithInternal) : Boolean {
        return transactionWithInternal.transaction.value == BigInteger.ZERO && transactionWithInternal.internalTransactions.isEmpty()
    }

    override fun refresh() {
        syncState = SyncState.Syncing()

        val lastTransaction = storage.getLastTransaction()
        val lastTransactionBlockHeight = lastTransaction?.blockNumber ?: 0
        val lastInternalTransactionBlockHeight = storage.getLastInternalTransactionBlockHeight() ?: 0

        Single.zip(
                transactionsProvider.getTransactions(lastTransactionBlockHeight + 1),
                transactionsProvider.getInternalTransactions(lastInternalTransactionBlockHeight + 1),
                BiFunction<List<Transaction>, List<InternalTransaction>, Pair<List<Transaction>, List<InternalTransaction>>> { t1, t2 -> Pair(t1, t2) })
                .subscribeOn(Schedulers.io())
                .subscribe({
                    update(it.first, it.second, lastTransaction?.hash)
                    syncState = SyncState.Synced()
                }, {
                    syncState = SyncState.NotSynced(it)
                }).let {
                    disposables.add(it)
                }
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionWithInternal>> {
        return storage.getTransactions(fromHash, limit)
    }

    override fun handle(transaction: Transaction) {
        storage.saveTransactions(listOf(transaction))

        val transactionWithInternal = TransactionWithInternal(transaction)

        if (!isEmpty(transactionWithInternal)) {
            listener?.onUpdateTransactions(listOf(transactionWithInternal))
        }
    }

}
