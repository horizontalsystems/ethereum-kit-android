package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

open class UnknownTransactionDecoration(
        val fromAddress: Address?,
        private val toAddress: Address?,
        private val userAddress: Address,
        private val value: BigInteger?,
        open val internalTransactions: List<InternalTransaction>,
        open val eventInstances: List<ContractEventInstance>
) : TransactionDecoration {

    override fun tags(): List<String> = (tagsFromInternalTransactions + tagsFromEventInstances).distinct()

    private val tagsFromInternalTransactions: List<String> by lazy {
        val incomingTxs = internalTransactions.filter { it.to == userAddress }
        val outgoingTxs = internalTransactions.filter { it.from == userAddress }

        var incomingValue = incomingTxs.sumOf { it.value }
        var outgoingValue = outgoingTxs.sumOf { it.value }

        value?.let {
            when (userAddress) {
                toAddress -> incomingValue += value
                fromAddress -> outgoingValue += value
            }
        }

        buildList {
            when {
                incomingValue > outgoingValue -> {
                    add(TransactionTag.EVM_COIN)
                    add(TransactionTag.EVM_COIN_INCOMING)
                    add(TransactionTag.INCOMING)
                }
                incomingValue < outgoingValue -> {
                    add(TransactionTag.EVM_COIN)
                    add(TransactionTag.EVM_COIN_OUTGOING)
                    add(TransactionTag.OUTGOING)
                }
            }

            internalTransactions.forEach { internalTransaction ->
                if (internalTransaction.from != userAddress) {
                    add(TransactionTag.fromAddress(internalTransaction.from.hex))
                }

                if (internalTransaction.to != userAddress) {
                    add(TransactionTag.toAddress(internalTransaction.to.hex))
                }
            }
        }
    }

    private val tagsFromEventInstances: List<String> by lazy {
        eventInstances.map { it.tags(userAddress) }.flatten()
    }

}