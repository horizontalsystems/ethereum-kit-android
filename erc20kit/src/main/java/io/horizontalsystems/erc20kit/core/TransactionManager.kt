package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.logging.Logger

class TransactionManager(
        private val contractAddress: ByteArray,
        private val address: ByteArray,
        private val storage: ITransactionStorage,
        private val transactionsProvider: ITransactionsProvider,
        private val dataProvider: IDataProvider,
        private val transactionBuilder: ITransactionBuilder) : ITransactionManager {

    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val logger = Logger.getLogger("TransactionManager")
    private val disposables = CompositeDisposable()

    override var listener: ITransactionManagerListener? = null

    override fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(fromTransaction, limit)
    }

    override fun sync() {
        val lastBlockHeight = dataProvider.lastBlockHeight
        val lastTransactionBlockHeight = storage.lastTransactionBlockHeight ?: 0

        transactionsProvider.getTransactions(contractAddress, address, lastTransactionBlockHeight + 1, lastBlockHeight)
                .subscribeOn(scheduler)
                .subscribe({ transactions ->
                    handleTransactions(transactions.filter { it.value != BigInteger.ZERO })
                }, {
                    logger.warning("Transaction sync error: ${it.javaClass.simpleName} ${it.message}")
                    listener?.onSyncTransactionsError(it)
                })
                .let {
                    disposables.add(it)
                }
    }

    private fun handleTransactions(transactions: List<Transaction>) {
        val pendingTransactions = storage.getPendingTransactions().toMutableList()

        transactions.forEach { transaction ->
            val pendingTransactionIndex = pendingTransactions.indexOfFirst {
                it.transactionHash.contentEquals(transaction.transactionHash)
                        && it.from.contentEquals(transaction.from)
                        && it.to.contentEquals(transaction.to)
            }

            if (pendingTransactionIndex > 0) {
                // when this transaction was sent interTransactionIndex was set to 0, so we need it to set 0 for it to replace the transaction in database
                transaction.interTransactionIndex = 0
                pendingTransactions.removeAt(pendingTransactionIndex)
            }
        }

        if (pendingTransactions.isEmpty()) {
            finishSync(transactions)
            return
        }

        dataProvider.getTransactionStatuses(pendingTransactions.map { it.transactionHash })
                .map { statuses ->
                    transactions.plus(getFailedTransactions(pendingTransactions, statuses))
                }
                .subscribeOn(scheduler)
                .subscribe({ allTransactions ->
                    finishSync(allTransactions)
                }, {
                    finishSync(transactions)
                }).let {
                    disposables.add(it)
                }
    }

    private fun getFailedTransactions(pendingTransactions: List<Transaction>,
                                      statuses: Map<ByteArray, TransactionStatus>): List<Transaction> {
        return statuses.mapNotNull { (hash, status) ->
            if (status == TransactionStatus.FAILED || status == TransactionStatus.NOTFOUND) {
                pendingTransactions.find { it.transactionHash.contentEquals(hash) }?.let { foundTx ->
                    foundTx.isError = true
                    foundTx
                }
            } else {
                null
            }
        }
    }

    private fun finishSync(transactions: List<Transaction>) {
        storage.save(transactions)
        listener?.onSyncSuccess(transactions)
    }

    override fun send(to: ByteArray, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        val transactionInput = transactionBuilder.transferTransactionInput(to, value)

        return dataProvider.send(contractAddress, transactionInput, gasPrice, gasLimit)
                .map { hash ->
                    Transaction(transactionHash = hash,
                            from = address,
                            to = to,
                            value = value)
                }.doOnSuccess { transaction ->
                    storage.save(listOf(transaction))
                }
    }

    override fun getTransactionInput(to: ByteArray, value: BigInteger): ByteArray {
        return transactionBuilder.transferTransactionInput(to, value)
    }
}
