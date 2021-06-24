package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.decorations.ApproveEventDecoration
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.erc20kit.models.*
import io.horizontalsystems.ethereumkit.core.*
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
        private val ethereumKit: EthereumKit,
        private val storage: ITransactionStorage
) {
    private val disposables = CompositeDisposable()
    private val transactionsSubject = PublishSubject.create<List<FullTransaction>>()
    private val address = ethereumKit.receiveAddress
    private val tags: List<List<String>> = listOf(listOf(contractAddress.hex))

    val transactionsAsync: Flowable<List<FullTransaction>> = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        ethereumKit.allTransactionsFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    processTransactions(it)
                }
                .let { disposables.add(it) }
    }

    fun sync() {
        val lastSyncOrder = storage.getTransactionSyncOrder()?.value
        val fullTransactions = ethereumKit.getFullTransactions(fromSyncOrder = lastSyncOrder)

        processTransactions(fullTransactions)
    }

    fun stop() {
        disposables.clear()
    }

    fun buildTransferTransactionData(to: Address, value: BigInteger): TransactionData {
        return TransactionData(to = contractAddress, value = BigInteger.ZERO, TransferMethod(to, value).encodedABI())
    }

    fun getTransactionsAsync(fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>> {
        return ethereumKit.getTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingTransactions(): List<FullTransaction> {
        return ethereumKit.getPendingTransactions(tags)
    }

    private fun processTransactions(fullTransactions: List<FullTransaction>) {
        val erc20Transactions = fullTransactions.filter { fullTransaction ->
            val transaction = fullTransaction.transaction

            fullTransaction.mainDecoration?.let { decoration ->
                return@filter when (decoration) {
                    is TransferMethodDecoration -> {
                        decoration.to == address || transaction.from == address
                    }
                    is ApproveMethodDecoration -> {
                        transaction.from == address
                    }
                    else -> false
                }
            }

            fullTransaction.eventDecorations.forEach { decoration ->
                return@filter when (decoration) {
                    is TransferEventDecoration -> {
                        decoration.from == address || decoration.to == address
                    }
                    is ApproveEventDecoration -> {
                        decoration.owner == address
                    }
                    else -> false
                }
            }

            return@filter false
        }

        if (erc20Transactions.isNotEmpty()) {
            transactionsSubject.onNext(erc20Transactions)
        }

        fullTransactions.maxByOrNull { it.transaction.syncOrder }?.let {
            storage.save(TransactionSyncOrder(it.transaction.syncOrder))
        }
    }

}
