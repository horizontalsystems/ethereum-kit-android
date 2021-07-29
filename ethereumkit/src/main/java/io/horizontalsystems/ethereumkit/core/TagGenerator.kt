package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.decorations.UnknownMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionTag

class TagGenerator(private val address: Address) {

    fun generate(fullTransaction: FullTransaction): List<TransactionTag> {
        val transaction = fullTransaction.transaction

        val toAddress = transaction.to
                ?: return listOf(TransactionTag("contractCreation", transaction.hash))

        val tags = mutableListOf<String>()

        if (transaction.from == address && transaction.value > 0.toBigInteger()) {
            tags.addAll(listOf(TransactionTag.EVM_COIN_OUTGOING, TransactionTag.EVM_COIN, TransactionTag.OUTGOING))
        }

        if (toAddress == address || fullTransaction.internalTransactions.any { it.to == address }) {
            tags.addAll(listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.EVM_COIN, TransactionTag.INCOMING))
        }

        fullTransaction.mainDecoration?.let { mainDecoration ->
            if (mainDecoration !is UnknownMethodDecoration) {
                tags.addAll(mainDecoration.tags(transaction.from, toAddress, address))
            }
        }

        fullTransaction.eventDecorations.forEach { event ->
            tags.addAll(event.tags(transaction.from, toAddress, address))
        }

        return tags.map { TransactionTag(it, transaction.hash) }
    }
}
