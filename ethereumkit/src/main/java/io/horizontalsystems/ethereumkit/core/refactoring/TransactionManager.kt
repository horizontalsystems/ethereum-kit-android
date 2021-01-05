package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class TransactionManager(
        private val address: Address,
        private val transactionSyncManager: TransactionSyncManager,
        private val storage: ITransactionStorage
) {
    private val disposables = CompositeDisposable()
    private val etherTransactionSubject = PublishSubject.create<List<FullTransaction>>()
    private val allTransactionsSubject = PublishSubject.create<List<FullTransaction>>()

    init {
        transactionSyncManager.transactionsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    etherTransactionSubject.onNext(transactions.filter { it.hasEtherTransfer(address) })
                    allTransactionsSubject.onNext(transactions)
                }
                .let { disposables.add(it) }
    }

    val etherTransactionsAsync: Flowable<List<FullTransaction>> = etherTransactionSubject.toFlowable(BackpressureStrategy.BUFFER)
    val allTransactionsAsync: Flowable<List<FullTransaction>> = allTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getEtherTransactionsAsync(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getEtherTransactionsAsync(address, fromHash, limit)
    }

    fun handle(transaction: Transaction) {
        storage.save(transaction)

        val fullTransaction = FullTransaction(transaction)
        if (fullTransaction.hasEtherTransfer(address)) {
            etherTransactionSubject.onNext(listOf(fullTransaction))
        }
        allTransactionsSubject.onNext(listOf(fullTransaction))

    }

    fun getFullTransactions(fromHash: ByteArray?): List<FullTransaction> {
        return storage.getFullTransactionsAfter(fromHash)
    }

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> {
        return storage.getFullTransactions(hashes)
    }


}
