package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class TransactionManager(private val storage: ITransactionStorage,
                         private val transactionsProvider: ITransactionsProvider) {

    interface Listener {
        fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>)
    }

    var listener: Listener? = null
    private var disposables = CompositeDisposable()

    private fun update(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions.filter { it.input.isEmpty() })
    }

    fun refresh() {
        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight() ?: 0

        transactionsProvider.getTransactions(lastTransactionBlockHeight + 1)
                .subscribeOn(Schedulers.io())
                .subscribe { transactions ->
                    update(transactions)
                }.let {
                    disposables.add(it)
                }
    }

    fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    fun handle(sentTransaction: EthereumTransaction) {
        update(listOf(sentTransaction))
    }

}
