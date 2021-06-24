package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
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
        private val decorationManager: DecorationManager,
        private val tagGenerator: TagGenerator
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
        return getTransactionsAsync(listOf(listOf("ETH")), fromHash, limit)
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

    fun getFullTransactions(fromSyncOrder: Long?): List<FullTransaction> {
        return storage.getFullTransactions(fromSyncOrder)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return storage.getFullTransactions(hashes)
    }

    private fun handle(syncedTransactions: List<FullTransaction>) {
        val decoratedTransactions = syncedTransactions.map { decorationManager.decorateFullTransaction(it) }
        val etherTransactions = mutableListOf<FullTransaction>()

        decoratedTransactions.forEach { transaction ->
            val tags = tagGenerator.generate(transaction)
            storage.set(tags)

            if (tags.any { it.name == "ETH" }) {
                etherTransactions.add(transaction)
            }
        }

        if (decoratedTransactions.isNotEmpty()) {
            allTransactionsSubject.onNext(decoratedTransactions)
        }

        if (etherTransactions.isNotEmpty()) {
            etherTransactionSubject.onNext(etherTransactions)
        }
    }

}
