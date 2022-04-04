package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData

class DecorationManager {
    private val decorators = mutableListOf<IDecorator>()

    fun addDecorator(decorator: IDecorator) {
        decorators.add(decorator)
    }

    fun decorateTransaction(transactionData: TransactionData): ContractMethodDecoration? {
        if (transactionData.input.isEmpty()) return null

        for (decorator in decorators) {
            decorator.decorate(transactionData)?.let {
                return it
            }
        }

        return null
    }

    fun decorateFullRpcTransaction(fullRpcTransaction: FullRpcTransaction): FullTransaction {
        val fullTransaction = FullTransaction(fullRpcTransaction.transaction)

        for (decorator in decorators) {
            decorator.decorate(fullTransaction, fullRpcTransaction)
        }

        decorateMain(fullTransaction)

        return fullTransaction
    }


    fun decorateTransactions(transactions: List<Transaction>): List<FullTransaction> {
        val fullTransactions: MutableMap<String, FullTransaction> = mutableMapOf()

        for (transaction in transactions) {
            fullTransactions[transaction.hashString] = FullTransaction(transaction)
        }

        for (decorator in decorators) {
            decorator.decorateTransactions(fullTransactions)
        }

        for (fullTransaction in fullTransactions.values) {
            decorateMain(fullTransaction)
        }

        return fullTransactions.values.toList()
    }

    private fun decorateMain(fullTransaction: FullTransaction) {
        if (fullTransaction.mainDecoration != null) return
        val transactionData = fullTransaction.transactionData ?: return

        val methodId = transactionData.input.take(4).toByteArray()
        val inputArguments = transactionData.input.takeLast(4).toByteArray()

        fullTransaction.mainDecoration = UnknownMethodDecoration(methodId, inputArguments)
    }

}
