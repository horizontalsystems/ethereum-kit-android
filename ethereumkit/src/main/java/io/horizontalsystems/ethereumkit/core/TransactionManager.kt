package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.transactionsyncers.TransactionSyncManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class TransactionManager(
        private val address: Address,
        transactionSyncManager: TransactionSyncManager,
        private val storage: ITransactionStorage,
        private val decorationManager: DecorationManager,
        private val tagGenerator: TagGenerator
) {
    private val disposables = CompositeDisposable()
    private val allTransactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val transactionsWithTagsSubject = PublishSubject.create<List<TransactionWithTags>>()

    init {
        transactionSyncManager.transactionsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    handle(transactions)
                }
                .let { disposables.add(it) }
    }

    val allTransactionsAsync: Flowable<List<FullTransaction>> = allTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getTransactionsFlowable(tags: List<List<String>>): Flowable<List<FullTransaction>> {
        return transactionsWithTagsSubject.toFlowable(BackpressureStrategy.BUFFER)
                .map { transactions ->
                    transactions.mapNotNull { transactionWithTags ->
                        for (andTags in tags) {
                            if (transactionWithTags.tags.all { !andTags.contains(it) }) {
                                return@mapNotNull null
                            }
                        }
                        return@mapNotNull transactionWithTags.transaction
                    }
                }
                .filter { it.isNotEmpty() }
    }

    fun getTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getTransactionsBeforeAsync(tags, fromHash, limit)
                .map { transactions ->
                    transactions.map { transaction ->
                        decorationManager.decorateFullTransaction(transaction)
                    }
                }
    }

    fun getPendingTransactions(tags: List<List<String>>): List<FullTransaction> {
        return storage.getPendingTransactions(tags)
                .map { transaction ->
                    decorationManager.decorateFullTransaction(transaction)
                }
    }

    fun handle(transaction: Transaction) {
        storage.save(transaction)

        val fullTransaction = FullTransaction(transaction)
        handle(listOf(fullTransaction))
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return storage.getFullTransactions(hashes)
    }

    private fun handle(syncedTransactions: List<FullTransaction>) {
        val decoratedTransactions = syncedTransactions.map { fullTransaction ->
            val decoratedTransaction = decorationManager.decorateFullTransaction(fullTransaction)
            decoratedTransaction.receiptWithLogs?.logs?.let { logs ->
                val neededLogs = logs.filter { it.relevant }
                if (logs.size > neededLogs.size) {
                    //delete all transactions, then save only relevant ones
                    storage.remove(logs)
                    storage.save(neededLogs)
                }
            }

            decoratedTransaction
        }

        val transactionWithTags = mutableListOf<TransactionWithTags>()

        decoratedTransactions.forEach { transaction ->
            val tags = tagGenerator.generate(transaction)
            storage.set(tags)

            transactionWithTags.add(TransactionWithTags(transaction, tags.map { it.name }))
        }

        if (decoratedTransactions.isNotEmpty()) {
            allTransactionsSubject.onNext(decoratedTransactions)
            transactionsWithTagsSubject.onNext(transactionWithTags)
        }
    }

    fun etherTransferTransactionData(address: Address, value: BigInteger): TransactionData {
        return TransactionData(address, value, byteArrayOf())
    }

    data class TransactionWithTags(
            val transaction: FullTransaction,
            val tags: List<String>
    )

}
