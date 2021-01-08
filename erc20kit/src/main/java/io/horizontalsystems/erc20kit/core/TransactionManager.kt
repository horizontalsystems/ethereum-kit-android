package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.models.Erc20LogEvent
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionCache
import io.horizontalsystems.erc20kit.models.TransactionType
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
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
        private val contractMethodFactories: ContractMethodFactories,
        private val storage: ITransactionStorage
) {
    private val disposables = CompositeDisposable()
    private val transactionsSubject = PublishSubject.create<List<Transaction>>()
    private val address = ethereumKit.receiveAddress

    val transactionsAsync: Flowable<List<Transaction>> = transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    init {
        ethereumKit.allTransactionsFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    processTransactions(it)
                }
                .let { disposables.add(it) }
    }

    fun sync() {
        val lastTransaction = storage.getLastTransaction()
        val fullTransactions = ethereumKit.getFullTransactions(lastTransaction?.hash)

        processTransactions(fullTransactions)
    }

    fun buildTransferTransactionData(to: Address, value: BigInteger): TransactionData {
        return TransactionData(to = contractAddress, value = BigInteger.ZERO, TransferMethod(to, value).encodedABI())
    }

    fun getTransactionsAsync(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(fromTransaction, limit)
                .map { cachedTransactions ->
                    val fullTransactions = ethereumKit.getFullTransactions(cachedTransactions.map { it.hash })
                    makeTransactions(cachedTransactions, fullTransactions)
                }
    }

    fun getPendingTransactions(): List<Transaction> {
        val pendingTransactions = storage.getPendingTransactions()
        val fullTransactions = ethereumKit.getFullTransactions(pendingTransactions.map { it.hash })

        return makeTransactions(pendingTransactions, fullTransactions)
    }

    private fun processTransactions(fullTransactions: List<FullTransaction>) {
        val cachedErc20Transaction = mutableListOf<TransactionCache>()

        fullTransactions.forEach {
            cachedErc20Transaction.addAll(extractErc20Transactions(it))
        }

        if (cachedErc20Transaction.isEmpty()) return

        val pendingTransactions = storage.getPendingTransactions().toMutableList()

        cachedErc20Transaction.forEach { transaction ->
            val pendingTransaction = pendingTransactions.firstOrNull {
                it.hash.contentEquals(transaction.hash)
            }

            if (pendingTransaction != null) {
                transaction.interTransactionIndex = pendingTransaction.interTransactionIndex
                storage.save(transaction)

                pendingTransactions.remove(pendingTransaction)
            } else {
                storage.save(transaction)
            }
        }

        val erc20Transactions: List<Transaction> = makeTransactions(cachedErc20Transaction, fullTransactions)

        transactionsSubject.onNext(erc20Transactions)
    }

    private fun makeTransactions(cachedErc20Transactions: List<TransactionCache>, fullTransactions: List<FullTransaction>): List<Transaction> {
        return cachedErc20Transactions.mapNotNull { transaction ->
            val fullTransaction = fullTransactions.firstOrNull { it.transaction.hash.contentEquals(transaction.hash) }

            fullTransaction?.let {
                Transaction(
                        transactionHash = transaction.hash,
                        interTransactionIndex = transaction.interTransactionIndex,
                        transactionIndex = fullTransaction.receiptWithLogs?.receipt?.transactionIndex,
                        from = transaction.from,
                        to = transaction.to,
                        value = transaction.value,
                        timestamp = transaction.timestamp,
                        isError = fullTransaction.isFailed(),
                        type = transaction.type,
                        fullTransaction = fullTransaction
                )
            }

        }
    }

    private fun extractErc20Transactions(fullTransaction: FullTransaction): List<TransactionCache> {
        val receiptWithLogs = fullTransaction.receiptWithLogs
        val transaction = fullTransaction.transaction

        return if (receiptWithLogs != null) {
            receiptWithLogs.logs.mapNotNull { log ->

                if (log.address != contractAddress) return@mapNotNull null

                val event = log.getErc20Event(address)

                val from: Address
                val to: Address
                val value: BigInteger
                val transactionType: TransactionType

                when (event) {
                    is Erc20LogEvent.Transfer -> {
                        from = event.from
                        to = event.to
                        value = event.value
                        transactionType = TransactionType.TRANSFER
                    }
                    is Erc20LogEvent.Approve -> {
                        from = event.owner
                        to = event.spender
                        value = event.value
                        transactionType = TransactionType.APPROVE
                    }
                    else -> return@mapNotNull null
                }

                TransactionCache(
                        hash = transaction.hash,
                        interTransactionIndex = log.logIndex,
                        logIndex = log.logIndex,
                        from = from,
                        to = to,
                        value = value,
                        timestamp = transaction.timestamp,
                        type = transactionType
                )
            }
        } else {
            if (transaction.to != contractAddress) return listOf()

            val contractMethod = contractMethodFactories.createMethodFromInput(fullTransaction.transaction.input)

            val from: Address
            val to: Address
            val value: BigInteger
            val transactionType: TransactionType

            when {
                (contractMethod is TransferMethod && (transaction.from == address || contractMethod.to == address)) -> {
                    from = transaction.from
                    to = contractMethod.to
                    value = contractMethod.value
                    transactionType = TransactionType.TRANSFER
                }
                (contractMethod is ApproveMethod && transaction.from == address) -> {
                    from = transaction.from
                    to = contractMethod.spender
                    value = contractMethod.value
                    transactionType = TransactionType.APPROVE
                }
                else -> return listOf()
            }

            listOf(TransactionCache(
                    hash = transaction.hash,
                    interTransactionIndex = 0,
                    logIndex = null,
                    from = from,
                    to = to,
                    value = value,
                    timestamp = transaction.timestamp,
                    type = transactionType
            ))
        }
    }
}
