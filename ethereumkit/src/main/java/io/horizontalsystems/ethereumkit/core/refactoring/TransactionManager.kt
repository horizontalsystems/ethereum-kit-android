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
import java.math.BigInteger


class TransactionManager(
        private val address: Address,
        private val transactionSyncManager: TransactionSyncManager,
        private val storage: IStorage
) {
    private val disposables = CompositeDisposable()
    private val etherTransactionSubject = PublishSubject.create<List<FullTransaction>>()

    init {
        transactionSyncManager.transactionsFlowable
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    etherTransactionSubject.onNext(transactions.filter { isEtherTransferred(it) })
                }
                .let { disposables.add(it) }
    }

    val etherTransactionsFlowable: Flowable<List<FullTransaction>> = etherTransactionSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun etherTransactionsSingle(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getEtherTransactionsAsync(address, fromHash, limit)
    }

    fun handle(transaction: Transaction) {
        storage.save(transaction)

        etherTransactionSubject.onNext(listOf(FullTransaction(transaction)))
    }

    private fun isEtherTransferred(fullTransaction: FullTransaction): Boolean =
            fullTransaction.transaction.from == address && fullTransaction.transaction.value > BigInteger.ZERO ||
                    fullTransaction.transaction.to == address ||
                    fullTransaction.internalTransactions.any { it.to == address }

}
