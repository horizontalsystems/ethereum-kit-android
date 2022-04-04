package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class TransactionManager(
        private val contractAddress: Address,
        private val ethereumKit: EthereumKit
) {
    private val disposables = CompositeDisposable()
    private val transactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val tags: List<List<String>> = listOf(listOf(contractAddress.hex))

    val transactionsAsync: Flowable<List<FullTransaction>> = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        ethereumKit.getFullTransactionsFlowable(tags)
                .subscribeOn(Schedulers.io())
                .subscribe {
                    processTransactions(it)
                }
                .let { disposables.add(it) }
    }

    fun stop() {
        disposables.clear()
    }

    fun buildTransferTransactionData(to: Address, value: BigInteger): TransactionData {
        return TransactionData(to = contractAddress, value = BigInteger.ZERO, TransferMethod(to, value).encodedABI())
    }

    fun getTransactionsAsync(fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>> {
        return ethereumKit.getFullTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingTransactions(): List<FullTransaction> {
        return ethereumKit.getPendingFullTransactions(tags)
    }

    private fun processTransactions(erc20Transactions: List<FullTransaction>) {
        if (erc20Transactions.isNotEmpty()) {
            transactionsSubject.onNext(erc20Transactions)
        }
    }

}
