package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class TransactionManager(
    private val address: Address,
    private val storage: ITransactionStorage,
    private val decorationManager: DecorationManager,
    private val blockchain: IBlockchain,
    private val provider: ITransactionProvider
) {

    private val fullTransactionsSubject = PublishSubject.create<Pair<List<FullTransaction>, Boolean>>()
    private val fullTransactionsWithTagsSubject = PublishSubject.create<List<TransactionWithTags>>()

    val fullTransactionsAsync: Flowable<Pair<List<FullTransaction>, Boolean>> = fullTransactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun getFullTransactionsFlowable(tags: List<List<String>>): Flowable<List<FullTransaction>> {
        return fullTransactionsWithTagsSubject.toFlowable(BackpressureStrategy.BUFFER)
            .map { transactions ->
                transactions.mapNotNull { transactionWithTags ->
                    for (andTags in tags) {
                        if (transactionWithTags.tags.all { !andTags.contains(it) }) {
                            return@mapNotNull null
                        }
                    }
                    return@mapNotNull transactionWithTags.transaction
                }
            }
            .filter { it.isNotEmpty() }
    }

    fun getFullTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> =
        storage.getTransactionsBeforeAsync(tags, fromHash, limit)
            .map { transactions ->
                decorationManager.decorateTransactions(transactions)
            }

    fun getPendingFullTransactions(tags: List<List<String>>): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getPendingTransactions(tags))

    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getTransactions(hashes))

    fun getDistinctTokenContractAddresses(): List<String> {
        return storage.getDistinctTokenContractAddresses().map {
            it
                .replace("_outgoing", "")
                .replace("_incoming", "")
        }
    }

    private fun save(transactions: List<Transaction>) {
        val existingTransactions = storage.getTransactions(hashes = transactions.map { it.hash }).associateBy { it.hashString }

        val mergedTransactions = transactions.map { newTx ->
            val existingTx = existingTransactions[newTx.hashString]

            if (existingTx != null) {
                Transaction(
                    hash = existingTx.hash,
                    timestamp = newTx.timestamp,
                    isFailed = existingTx.isFailed || newTx.isFailed,

                    blockNumber = newTx.blockNumber ?: existingTx.blockNumber,
                    transactionIndex = newTx.transactionIndex ?: existingTx.transactionIndex,
                    from = newTx.from ?: existingTx.from,
                    to = newTx.to ?: existingTx.to,
                    value = newTx.value ?: existingTx.value,
                    input = newTx.input ?: existingTx.input,
                    nonce = newTx.nonce ?: existingTx.nonce,
                    gasPrice = newTx.gasPrice ?: existingTx.gasPrice,
                    maxFeePerGas = newTx.maxFeePerGas ?: existingTx.maxFeePerGas,
                    maxPriorityFeePerGas = newTx.maxPriorityFeePerGas ?: existingTx.maxPriorityFeePerGas,
                    gasLimit = newTx.gasLimit ?: existingTx.gasLimit,
                    gasUsed = newTx.gasUsed ?: existingTx.gasUsed,

                    replacedWith = newTx.replacedWith ?: existingTx.replacedWith
                )
            } else {
                newTx
            }
        }

        storage.save(mergedTransactions)
    }

    fun handle(transactions: List<Transaction>, initial: Boolean = false): List<FullTransaction> {
        if (transactions.isEmpty()) return listOf()

        save(transactions)
        val failedTransactions = failPendingTransactions()
        val fullTransactions = decorationManager.decorateTransactions(transactions + failedTransactions)

        val transactionWithTags = mutableListOf<TransactionWithTags>()
        val allTags = mutableListOf<TransactionTag>()

        fullTransactions.forEach { fullTransaction ->
            val tags = fullTransaction.decoration.tags()
            val transactionHash = fullTransaction.transaction.hash
            val transactionTags = tags.map { TransactionTag(it, transactionHash) }

            allTags.addAll(transactionTags)
            transactionWithTags.add(TransactionWithTags(fullTransaction, tags))
        }

        storage.saveTags(allTags)

        fullTransactionsSubject.onNext(Pair(fullTransactions, initial))
        fullTransactionsWithTagsSubject.onNext(transactionWithTags)

        return fullTransactions
    }

    fun etherTransferTransactionData(address: Address, value: BigInteger): TransactionData {
        return TransactionData(address, value, byteArrayOf())
    }

    fun getFullTransactionSingle(hash: ByteArray): Single<FullTransaction> {
        val fullRpcTransactionSingle = blockchain.getTransaction(hash)
            .flatMap { transaction ->
                if (transaction.blockNumber != null) {
                    return@flatMap Single.zip(
                        blockchain.getTransactionReceipt(hash),
                        blockchain.getBlock(transaction.blockNumber),
                        provider.getInternalTransactionsAsync(hash)
                    ) { receipt, block, internalTransactions ->
                        FullRpcTransaction(transaction, receipt, block, internalTransactions.map { it.internalTransaction() }.toMutableList())
                    }
                } else {
                    return@flatMap Single.just(FullRpcTransaction(transaction, null, null, mutableListOf()))
                }
            }

        return fullRpcTransactionSingle.map { decorationManager.decorateFullRpcTransaction(it) }
    }

    fun getFullTransactionsAfterSingle(fromHash: ByteArray? = null): Single<List<FullTransaction>> =
        storage.getTransactionsAfterSingle(fromHash)
            .map { transactions ->
                decorationManager.decorateTransactions(transactions)
            }

    private fun failPendingTransactions(): List<Transaction> {
        val pendingTransactions = storage.getPendingTransactions()

        if (pendingTransactions.isEmpty()) return listOf()

        val pendingTransactionNonces = pendingTransactions.mapNotNull { it.nonce }.toSet().toList()
        val nonPendingTransactions = storage.getNonPendingTransactionsByNonces(address, pendingTransactionNonces)
        val processedTransactions: MutableList<Transaction> = mutableListOf()

        for (nonPendingTransaction in nonPendingTransactions) {
            val duplicateTransactions = pendingTransactions.filter { it.nonce == nonPendingTransaction.nonce }
            for (transaction in duplicateTransactions) {
                transaction.isFailed = true
                transaction.replacedWith = nonPendingTransaction.hash
                processedTransactions.add(transaction)
            }
        }

        save(processedTransactions)
        return processedTransactions
    }

    data class TransactionWithTags(
        val transaction: FullTransaction,
        val tags: List<String>
    )

}
