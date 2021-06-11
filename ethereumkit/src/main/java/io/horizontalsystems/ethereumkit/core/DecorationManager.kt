package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData

class DecorationManager(
        private val address: Address
) {
    private val decorators = mutableListOf<IDecorator>()

    fun addDecorator(decorator: IDecorator) {
        decorators.add(decorator)
    }

    fun decorateTransaction(transactionData: TransactionData): TransactionDecoration? {
        if (transactionData.input.isEmpty())
            return null

        for (decorator in decorators) {
            decorator.decorate(transactionData, null)?.let {
                return it
            }
        }
        return null
    }

    fun decorateFullTransaction(fullTransaction: FullTransaction): FullTransaction {
        val transaction = fullTransaction.transaction
        val toAddress = transaction.to ?: return fullTransaction
        val transactionData = TransactionData(toAddress, transaction.value, transaction.input)

        if (transactionData.input.isEmpty())
            return fullTransaction

        for (decorator in decorators) {
            decorator.decorate(transactionData, fullTransaction)?.let {
                fullTransaction.mainDecoration = it
            }
            fullTransaction.receiptWithLogs?.let{
                fullTransaction.eventDecorations.addAll(decorator.decorate(it.logs))
            }
        }

        if (fullTransaction.mainDecoration == null) {
            val methodId = fullTransaction.transaction.input.take(4).toByteArray()
            val inputArguments = fullTransaction.transaction.input.takeLast(4).toByteArray()

            fullTransaction.mainDecoration = TransactionDecoration.Unknown(methodId, inputArguments)
        }

        return fullTransaction
    }

}
