package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import java.math.BigInteger

class FullTransaction(
    val transaction: Transaction
) {
    var internalTransactions: MutableList<InternalTransaction> = mutableListOf()
    var mainDecoration: ContractMethodDecoration? = null
    var eventDecorations: MutableList<ContractEventDecoration> = mutableListOf()

    val transactionData: TransactionData?
        get() {
            if (transaction.input == null || transaction.to == null || transaction.value == null || transaction.input.isEmpty()) return null

            return TransactionData(transaction.to, transaction.value, transaction.input)
        }

    fun hasEtherTransfer(address: Address): Boolean =
        transaction.value != null && (
            transaction.from == address && transaction.value > BigInteger.ZERO ||
                transaction.to == address ||
                internalTransactions.any { it.to == address }
            )

}
