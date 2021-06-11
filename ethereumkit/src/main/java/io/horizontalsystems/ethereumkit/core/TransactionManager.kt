package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.ethereumkit.transactionsyncers.TransactionSyncManager
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class TransactionManager(
        private val address: Address,
        transactionSyncManager: TransactionSyncManager,
        private val storage: ITransactionStorage,
        private val decorationManager: DecorationManager
) {
    private val disposables = CompositeDisposable()
    private val etherTransactionSubject = PublishSubject.create<List<FullTransaction>>()
    private val allTransactionsSubject = PublishSubject.create<List<FullTransaction>>()

    init {
        transactionSyncManager.transactionsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    handle(transactions)
                }
                .let { disposables.add(it) }
    }

    val etherTransactionsAsync: Flowable<List<FullTransaction>> = etherTransactionSubject.toFlowable(BackpressureStrategy.BUFFER)
    val allTransactionsAsync: Flowable<List<FullTransaction>> = allTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getEtherTransactionsAsync(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getEtherTransactionsAsync(address, fromHash, limit)
                .map { transactions ->
                    transactions.map { transaction ->
                        decorationManager.decorateFullTransaction(transaction)
                    }
                }
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
        if (fullTransaction.hasEtherTransfer(address)) {
            etherTransactionSubject.onNext(listOf(fullTransaction))
        }
        allTransactionsSubject.onNext(listOf(fullTransaction))
    }

    fun getFullTransactions(fromSyncOrder: Long?): List<FullTransaction> {
        return storage.getFullTransactions(fromSyncOrder)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return storage.getFullTransactions(hashes)
    }

    private fun handle(syncedTransactions: List<FullTransaction>) {
        val decoratedTransactions = syncedTransactions.map { decorationManager.decorateFullTransaction(it) }

        decoratedTransactions.forEach { transaction ->
            updateTags(transaction)
        }

        if (decoratedTransactions.isNotEmpty()) {
            allTransactionsSubject.onNext(decoratedTransactions)
        }

        val etherTransactions = decoratedTransactions.filter { it.hasEtherTransfer(address) }
        if (etherTransactions.isNotEmpty()) {
            etherTransactionSubject.onNext(etherTransactions)
        }
    }

    private fun updateTags(transaction: FullTransaction) {
        val tags = mutableListOf<TransactionTag>()

        transaction.mainDecoration?.name?.let {
            tags.add(TransactionTag(it, transaction.transaction.hash))
        }

        transaction.transaction.to?.let {
            tags.add(TransactionTag(it.hex, transaction.transaction.hash))
        }

        transaction.eventDecorations.forEach { eventDecoration ->
            tags.addAll(eventDecoration.tags.map { TransactionTag(it, transaction.transaction.hash) })
        }

        storage.set(tags)
    }

}
