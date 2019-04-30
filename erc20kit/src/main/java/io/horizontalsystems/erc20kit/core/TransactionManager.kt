package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class TransactionManager(private val contractAddress: ByteArray,
                         private val address: ByteArray,
                         private val storage: ITransactionStorage,
                         private val dataProvider: IDataProvider,
                         private val transactionBuilder: ITransactionBuilder) : ITransactionManager {

    private val disposables = CompositeDisposable()

    override var listener: ITransactionManagerListener? = null

    override val lastTransactionBlockHeight: Long?
        get() = storage.lastTransactionBlockHeight

    override fun transactionsSingle(hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(hashFrom, indexFrom, limit)
    }

    override fun sync() {
        val lastBlockHeight = dataProvider.lastBlockHeight
        val lastTransactionBlockHeight = storage.lastTransactionBlockHeight ?: 0

        dataProvider.getTransactions(contractAddress, address, lastTransactionBlockHeight + 1, lastBlockHeight)
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    storage.save(transactions)
                    listener?.onSyncSuccess(transactions)
                }, {
                    listener?.onSyncTransactionsError()
                })
                .let {
                    disposables.add(it)
                }
    }

    override fun sendSingle(to: ByteArray, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        val transactionInput = transactionBuilder.transferTransactionInput(to, value)

        return dataProvider.sendSingle(contractAddress, transactionInput, gasPrice, gasLimit)
                .map { hash ->
                    Transaction(transactionHash = hash,
                            contractAddress = contractAddress,
                            from = address,
                            to = to,
                            value = value)
                }.doOnSuccess { transaction ->
                    storage.save(listOf(transaction))
                }
    }

    override fun clear() {
        disposables.clear()
        storage.clearTransactions()
    }
}
