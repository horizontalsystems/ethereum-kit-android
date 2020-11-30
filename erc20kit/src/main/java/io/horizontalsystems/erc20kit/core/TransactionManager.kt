package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

class TransactionManager(
        private val contractAddress: Address,
        private val address: Address,
        private val storage: ITransactionStorage,
        private val transactionsProvider: ITransactionsProvider,
        private val dataProvider: IDataProvider,
        private val transactionBuilder: ITransactionBuilder) : ITransactionManager {

    private val scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
    private val logger = Logger.getLogger("Erc20TransactionManager")
    private val disposables = CompositeDisposable()

    private var delayTime = 3 //in seconds
    private var delayTimeIncreaseFactor = 2
    private var retryCount = 0
    private var syncing: AtomicBoolean = AtomicBoolean(false)

    override var listener: ITransactionManagerListener? = null

    override fun getTransactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(fromTransaction, limit)
    }

    override fun getPendingTransactions(): List<Transaction> {
        return storage.getPendingTransactions()
    }

    override fun immediateSync() {
        if (syncing.getAndSet(true)) return

        listener?.onSyncStarted()
        sync()
    }

    override fun delayedSync(expectTransaction: Boolean) {
        if (syncing.getAndSet(true)) return

        retryCount = if (expectTransaction) 5 else 3
        delayTime = 3

        listener?.onSyncStarted()
        sync(delayTime)
    }

    override fun send(to: Address, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
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

    override fun getTransactionInput(to: Address, value: BigInteger): ByteArray {
        return transactionBuilder.transferTransactionInput(to, value)
    }

    private fun sync(delayTime: Int? = null) {
        val lastBlockHeight = dataProvider.lastBlockHeight
        val lastTransactionBlockHeight = storage.lastTransactionBlockHeight ?: 0

        var single = transactionsProvider
                .getTransactions(contractAddress, address, lastTransactionBlockHeight + 1, lastBlockHeight)
                .flatMap {
                    processTransactions(it)
                }

        delayTime?.let {
            single = single.delaySubscription(delayTime.toLong(), TimeUnit.SECONDS)
        }

        single.subscribeOn(scheduler)
                .subscribe({ transactions ->
                    finishSync(transactions)
                }, { error ->
                    finishSync(error)
                }).let {
                    disposables.add(it)
                }
    }

    private fun processTransactions(transactions: List<Transaction>): Single<List<Transaction>> {
        val pendingTransactions = storage.getPendingTransactions().toMutableList()

        transactions.forEach { transaction ->
            val pendingTransactionIndex = pendingTransactions.indexOfFirst {
                it.transactionHash.contentEquals(transaction.transactionHash) && it.from == transaction.from && it.to == transaction.to
            }

            if (pendingTransactionIndex > 0) {
                // when this transaction was sent interTransactionIndex was set to 0, so we need it to set 0 for it to replace the transaction in database
                transaction.interTransactionIndex = 0
                pendingTransactions.removeAt(pendingTransactionIndex)
            }
        }

        if (pendingTransactions.isEmpty()) {
            return Single.just(transactions)
        }

        return dataProvider.getTransactionStatuses(pendingTransactions.map { it.transactionHash })
                .map { statuses ->
                    transactions.plus(getFailedTransactions(pendingTransactions, statuses))
                }
                .onErrorResumeNext {
                    Single.just(transactions)
                }
    }

    private fun getFailedTransactions(pendingTransactions: List<Transaction>,
                                      statuses: Map<ByteArray, TransactionStatus>): List<Transaction> {
        return statuses.mapNotNull { (hash, status) ->
            if (status == TransactionStatus.FAILED) {
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
        if (retryCount > 0 && transactions.isEmpty()) {
            retryCount -= 1
            delayTime *= delayTimeIncreaseFactor
            sync(delayTime)
            return
        }
        syncing.set(false)
        retryCount = 0
        storage.save(transactions)
        listener?.onSyncSuccess(transactions)
    }

    private fun finishSync(error: Throwable) {
        logger.warning("Transaction sync error: ${error.javaClass.simpleName} ${error.message}")

        syncing.set(false)
        retryCount = 0
        listener?.onSyncTransactionsError(error)
    }

}
