package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.core.ITransactionStorage
import io.horizontalsystems.ethereumkit.models.FullRpcTransaction
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData

class InternalTransactionsDecorator(private val storage: ITransactionStorage): IDecorator {

    override fun decorate(transactionData: TransactionData): ContractMethodDecoration? = null

    override fun decorate(fullTransaction: FullTransaction, fullRpcTransaction: FullRpcTransaction) {
        fullTransaction.internalTransactions = fullRpcTransaction.internalTransactions
    }

    override fun decorateTransactions(fullTransactions: Map<String, FullTransaction>) {
        val internalTransactions = if (fullTransactions.size > 100) {
            storage.getInternalTransactions()
        } else {
            storage.getInternalTransactionsByHashes(fullTransactions.values.map { it.transaction.hash })
        }

        for (internalTx in internalTransactions) {
            fullTransactions[internalTx.hashString]?.internalTransactions?.add(internalTx)
        }
    }

}
