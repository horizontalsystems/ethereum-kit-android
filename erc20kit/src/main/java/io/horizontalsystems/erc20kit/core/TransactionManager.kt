package io.horizontalsystems.erc20kit.core

import android.util.Log
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class TransactionManager(private val address: ByteArray,
                         private val storage: ITransactionStorage,
                         private val dataProvider: IDataProvider,
                         private val transactionBuilder: ITransactionBuilder) : ITransactionManager {

    private val disposables = CompositeDisposable()

    override var listener: ITransactionManagerListener? = null

    override fun lastTransactionBlockHeight(contractAddress: ByteArray): Long? {
        return storage.lastTransactionBlockHeight
    }

    override fun transactionsSingle(contractAddress: ByteArray, hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(contractAddress, hashFrom, indexFrom, limit)
    }

    override fun sync() {
        val lastBlockHeight = dataProvider.lastBlockHeight
        val lastTransactionBlockHeight = storage.lastTransactionBlockHeight ?: 0

        dataProvider.getTransactions(lastTransactionBlockHeight + 1, lastBlockHeight, address)
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

    override fun sendSingle(contractAddress: ByteArray, to: ByteArray, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
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
