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
        private val storage: IStorage
) {
    private val disposables = CompositeDisposable()
    private val etherTransactionSubject = PublishSubject.create<List<FullTransaction>>()

    init {
        transactionSyncManager.transactionsAsync
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    etherTransactionSubject.onNext(transactions.filter { it.hasEtherTransfer(address)})
                }
                .let { disposables.add(it) }
    }

    val etherTransactionsAsync: Flowable<List<FullTransaction>> = etherTransactionSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getEtherTransactionsAsync(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getEtherTransactionsAsync(address, fromHash, limit)
    }

    fun handle(transaction: Transaction) {
        storage.save(transaction)

        etherTransactionSubject.onNext(listOf(FullTransaction(transaction)))
    }

}
