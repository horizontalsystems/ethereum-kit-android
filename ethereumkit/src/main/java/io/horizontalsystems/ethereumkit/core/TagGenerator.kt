package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionTag

class TagGenerator(private val address: Address) {

    fun generate(fullTransaction: FullTransaction): List<TransactionTag> {
        val tagNames = generateFromMain(fullTransaction) + generateFromEvents(fullTransaction)

        return tagNames.toSet().toList().map { TransactionTag(it, fullTransaction.transaction.hash) }
    }

    private fun generateFromMain(fullTransaction: FullTransaction): List<String> {
        val transaction = fullTransaction.transaction

        transaction.from ?: return listOf()
        transaction.value ?: return listOf()
        transaction.to ?: return listOf("contractCreation")

        val tags = mutableListOf<String>()

        if (transaction.value > 0.toBigInteger() && transaction.from == address) {
            tags.addAll(listOf(TransactionTag.EVM_COIN_OUTGOING, TransactionTag.EVM_COIN, TransactionTag.OUTGOING))
        }

        if (transaction.to == address || fullTransaction.internalTransactions.any { it.to == address }) {
            tags.addAll(listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.EVM_COIN, TransactionTag.INCOMING))
        }

        fullTransaction.mainDecoration?.let { mainDecoration ->
            tags.addAll(mainDecoration.tags(transaction.from, transaction.to, address))
        }

        return tags
    }

    private fun generateFromEvents(fullTransaction: FullTransaction): List<String> {
        val tags = mutableListOf<String>()

        fullTransaction.eventDecorations.forEach { event ->
            tags.addAll(event.tags(address))
        }

        return tags
    }

}
