package io.horizontalsystems.erc20kit.core.refactoring

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.core.ITransactionStorage
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionRecord
import io.horizontalsystems.erc20kit.models.TransactionType
import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.contracts.ContractEvent.Argument
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger


class TransactionManagerNew(
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

    fun getTransactionInput(to: Address, value: BigInteger): ByteArray {
        return TransferMethod(to, value).encodedABI()
    }

    fun getTransactionsAsync(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return storage.getTransactions(fromTransaction, limit)
                .map { transactionRecords ->
                    val fullTransactions = ethereumKit.getFullTransactions(transactionRecords.map { it.hash })
                    makeTransactions(transactionRecords, fullTransactions)
                }
    }

    fun getPendingTransactions(): List<Transaction> {
        val transactionRecords = storage.getPendingTransactions()
        val fullTransactions = ethereumKit.getFullTransactions(transactionRecords.map { it.hash })

        return makeTransactions(transactionRecords, fullTransactions)
    }

    private fun processTransactions(fullTransactions: List<FullTransaction>) {
        val erc20TransactionRecords = mutableListOf<TransactionRecord>()

        fullTransactions.forEach {
            erc20TransactionRecords.addAll(extractErc20TransactionRecords(it))
        }

        if (erc20TransactionRecords.isEmpty()) return

        val pendingTransactionRecords = storage.getPendingTransactions().toMutableList()

        erc20TransactionRecords.forEach { transaction ->
            val pendingTransactionRecord = pendingTransactionRecords.firstOrNull {
                it.hash.contentEquals(transaction.hash)
            }

            if (pendingTransactionRecord != null) {
                transaction.interTransactionIndex = pendingTransactionRecord.interTransactionIndex
                storage.save(transaction)

                pendingTransactionRecords.remove(pendingTransactionRecord)
            } else {
                storage.save(transaction)
            }
        }

        val erc20Transactions: List<Transaction> = makeTransactions(erc20TransactionRecords, fullTransactions)

        transactionsSubject.onNext(erc20Transactions)
    }

    private fun makeTransactions(erc20TransactionRecords: List<TransactionRecord>, fullTransactions: List<FullTransaction>): List<Transaction> {
        return erc20TransactionRecords.mapNotNull { transactionRecord ->
            val fullTransaction = fullTransactions.firstOrNull { it.transaction.hash.contentEquals(transactionRecord.hash) }

            fullTransaction?.let {
                Transaction(
                        transactionHash = transactionRecord.hash,
                        interTransactionIndex = transactionRecord.interTransactionIndex,
                        transactionIndex = fullTransaction.receiptWithLogs?.receipt?.transactionIndex,
                        from = transactionRecord.from,
                        to = transactionRecord.to,
                        value = transactionRecord.value,
                        timestamp = transactionRecord.timestamp,
                        isError = fullTransaction.isFailed(),
                        type = transactionRecord.type,
                        fullTransaction = fullTransaction
                )
            }

        }
    }

    private fun extractErc20TransactionRecords(fullTransaction: FullTransaction): List<TransactionRecord> {
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

                TransactionRecord(
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

            listOf(TransactionRecord(
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

    sealed class Erc20LogEvent {
        class Transfer(val from: Address, val to: Address, val value: BigInteger) : Erc20LogEvent() {
            companion object {
                val signature = ContractEvent("Transfer", listOf(Argument.Address, Argument.Address, Argument.Uint256)).signature
            }
        }

        class Approve(val owner: Address, val spender: Address, val value: BigInteger) : Erc20LogEvent() {
            companion object {
                val signature = ContractEvent("Approval", listOf(Argument.Address, Argument.Address, Argument.Uint256)).signature
            }
        }
    }

    private fun TransactionLog.getErc20Event(address: Address): Erc20LogEvent? {
        return try {
            val signature = topics.getOrNull(0)?.hexStringToByteArrayOrNull()
            val firstParam = topics.getOrNull(1)?.let { Address(it.stripHexPrefix().removeLeadingZeros()) }
            val secondParam = topics.getOrNull(2)?.let { Address(it.stripHexPrefix().removeLeadingZeros()) }

            when {
                signature.contentEquals(Erc20LogEvent.Transfer.signature) && firstParam != null && secondParam != null && (firstParam == address || secondParam == address) -> {
                    Erc20LogEvent.Transfer(firstParam, secondParam, BigInteger(data))
                }
                signature.contentEquals(Erc20LogEvent.Approve.signature) && firstParam == address && secondParam != null -> {
                    Erc20LogEvent.Approve(firstParam, secondParam, BigInteger(data))
                }
                else -> null
            }
        } catch (error: Throwable) {
            error.printStackTrace()
            null
        }
    }
}
