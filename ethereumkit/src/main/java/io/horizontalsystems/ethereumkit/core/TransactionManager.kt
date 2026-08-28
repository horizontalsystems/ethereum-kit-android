package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.DecorationManager
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.math.BigInteger

class TransactionManager(
    private val address: Address,
    private val storage: ITransactionStorage,
    private val decorationManager: DecorationManager,
    private val blockchain: IBlockchain,
    private val provider: ITransactionProvider
) {

    private val fullTransactionsSubject = bufferedSharedFlow<Pair<List<FullTransaction>, Boolean>>()
    private val fullTransactionsWithTagsSubject = bufferedSharedFlow<List<TransactionWithTags>>()

    val fullTransactionsFlow: Flow<Pair<List<FullTransaction>, Boolean>> = fullTransactionsSubject

    fun getFullTransactionsFlow(tags: List<List<String>>): Flow<List<FullTransaction>> {
        return fullTransactionsWithTagsSubject
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

    suspend fun getFullTransactionsAsync(tags: List<List<String>>, fromHash: ByteArray? = null, limit: Int? = null): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getTransactionsBeforeAsync(tags, fromHash, limit))

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

        fullTransactionsSubject.tryEmit(Pair(fullTransactions, initial))
        fullTransactionsWithTagsSubject.tryEmit(transactionWithTags)

        return fullTransactions
    }

    fun etherTransferTransactionData(address: Address, value: BigInteger): TransactionData {
        return TransactionData(address, value, byteArrayOf())
    }

    suspend fun getFullTransaction(hash: ByteArray): FullTransaction {
        val transaction = blockchain.getTransaction(hash)

        val fullRpcTransaction = if (transaction.blockNumber != null) {
            coroutineScope {
                val receipt = async { blockchain.getTransactionReceipt(hash) }
                val block = async { blockchain.getBlock(transaction.blockNumber) }
                val internalTransactions = async { provider.getInternalTransactionsAsync(hash) }

                FullRpcTransaction(
                    transaction,
                    receipt.await(),
                    block.await(),
                    internalTransactions.await().map { it.internalTransaction() }.toMutableList()
                )
            }
        } else {
            FullRpcTransaction(transaction, null, null, mutableListOf())
        }

        return withContext(Dispatchers.IO) {
            decorationManager.decorateFullRpcTransaction(fullRpcTransaction)
        }
    }

    suspend fun getFullTransactionsAfter(fromHash: ByteArray? = null): List<FullTransaction> =
        decorationManager.decorateTransactions(storage.getTransactionsAfter(fromHash))

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
