package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.EmptyMethod
import io.horizontalsystems.ethereumkit.core.IEventDecorator
import io.horizontalsystems.ethereumkit.core.IExtraDecorator
import io.horizontalsystems.ethereumkit.core.IMethodDecorator
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog
import java.math.BigInteger

class DecorationManager(private val userAddress: Address, private val storage: ITransactionStorage) {
    private val methodDecorators = mutableListOf<IMethodDecorator>()
    private val eventDecorators = mutableListOf<IEventDecorator>()
    private val transactionDecorators = mutableListOf<ITransactionDecorator>()
    private val extraDecorators = mutableListOf<IExtraDecorator>()

    private fun extra(hash: ByteArray) = buildMap {
        extraDecorators.forEach { extraDecorator ->
            putAll(extraDecorator.extra(hash))
        }
    }

    fun addExtraDecorator(decorator: IExtraDecorator) {
        extraDecorators.add(decorator)
    }

    fun addMethodDecorator(decorator: IMethodDecorator) {
        methodDecorators.add(decorator)
    }

    fun addEventDecorator(decorator: IEventDecorator) {
        eventDecorators.add(decorator)
    }

    fun addTransactionDecorator(decorator: ITransactionDecorator) {
        transactionDecorators.add(decorator)
    }

    fun decorateTransaction(from: Address, transactionData: TransactionData): TransactionDecoration? {
        val contractMethod = contractMethod(transactionData.input) ?: return null

        for (decorator in transactionDecorators) {
            val decoration = decorator.decoration(from, transactionData.to, transactionData.value, contractMethod, listOf(), listOf())
            if (decoration != null) return decoration
        }

        return null
    }

    fun decorateTransactions(transactions: List<Transaction>): List<FullTransaction> {
        val internalTransactionsMap: MutableMap<String, List<InternalTransaction>> = getInternalTransactionsMap(transactions).toMutableMap()
        val eventInstancesMap: MutableMap<String, List<ContractEventInstance>> = mutableMapOf()

        for (decorator in eventDecorators) {
            for ((hash, eventInstances) in decorator.contractEventInstancesMap(transactions)) {
                eventInstancesMap[hash] = (eventInstancesMap[hash] ?: listOf()) + eventInstances
            }
        }

        return transactions.map { transaction ->
            val decoration = decoration(
                transaction.from,
                transaction.to,
                transaction.value,
                contractMethod(transaction.input),
                internalTransactionsMap[transaction.hashString] ?: listOf(),
                eventInstancesMap[transaction.hashString] ?: listOf()
            )

            return@map FullTransaction(transaction, decoration, extra(transaction.hash))
        }
    }

    fun decorateFullRpcTransaction(fullRpcTransaction: FullRpcTransaction): FullTransaction {
        val timestamp = if (fullRpcTransaction.rpcBlock != null) {
            fullRpcTransaction.rpcBlock.timestamp
        } else {
            val transaction = storage.getTransaction(fullRpcTransaction.rpcTransaction.hash)
            transaction?.timestamp ?: throw Exception("Transaction not in DB")
        }

        val transaction = fullRpcTransaction.transaction(timestamp)

        val decoration = decoration(
            transaction.from,
            transaction.to,
            transaction.value,
            contractMethod(transaction.input),
            fullRpcTransaction.internalTransactions,
            fullRpcTransaction.rpcReceipt?.logs?.let { eventInstances(it) } ?: listOf()
        )

        return FullTransaction(transaction, decoration, extra(transaction.hash))
    }

    private fun getInternalTransactionsMap(transactions: List<Transaction>): Map<String, List<InternalTransaction>> {
        val internalTransactions: List<InternalTransaction> = if (transactions.size > 100) {
            storage.getInternalTransactions()
        } else {
            val hashes = transactions.map { it.hash }
            storage.getInternalTransactionsByHashes(hashes)
        }

        val map: MutableMap<String, List<InternalTransaction>> = mutableMapOf()

        for (internalTransaction in internalTransactions) {
            map[internalTransaction.hashString] = (map[internalTransaction.hashString] ?: mutableListOf()) + listOf(internalTransaction)
        }

        return map
    }

    private fun contractMethod(input: ByteArray?): ContractMethod? {
        if (input == null) return null
        if (input.isEmpty()) return EmptyMethod()

        for (decorator in methodDecorators) {
            val contractMethod = decorator.contractMethod(input)

            if (contractMethod != null) return contractMethod
        }

        return null
    }


    private fun decoration(from: Address?, to: Address?, value: BigInteger?, contractMethod: ContractMethod? = null, internalTransactions: List<InternalTransaction> = listOf(), eventInstances: List<ContractEventInstance> = listOf()): TransactionDecoration {
        for (decorator in transactionDecorators) {
            val decoration = decorator.decoration(from, to, value, contractMethod, internalTransactions, eventInstances)
            if (decoration != null) return decoration
        }

        return UnknownTransactionDecoration(from, to, userAddress, value, internalTransactions, eventInstances)
    }

    private fun eventInstances(logs: List<TransactionLog>): List<ContractEventInstance> {
        val eventInstances: MutableList<ContractEventInstance> = mutableListOf()

        for (decorator in eventDecorators) {
            eventInstances.addAll(decorator.contractEventInstances(logs))
        }

        return eventInstances
    }

}
