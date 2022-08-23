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
) : TransactionDecoration() {

    override fun tags(): List<String> = (tagsFromInternalTransactions + tagsFromEventInstances).toSet().toList()

    private val tagsFromInternalTransactions: List<String>
        get() {
            var outgoingValue = if (fromAddress == userAddress) value ?: BigInteger.ZERO else BigInteger.ZERO
            for (internalTx in internalTransactions.filter { it.from == userAddress }) {
                outgoingValue += internalTx.value
            }

            var incomingValue = if (toAddress == userAddress) value ?: BigInteger.ZERO else BigInteger.ZERO
            for (internalTx in internalTransactions.filter { it.to == userAddress }) {
                incomingValue += internalTx.value
            }

            if (incomingValue == BigInteger.ZERO && outgoingValue == BigInteger.ZERO) return listOf()

            val tags = mutableListOf(TransactionTag.EVM_COIN)

            if (incomingValue > outgoingValue) {
                tags.add(TransactionTag.EVM_COIN_INCOMING)
                tags.add(TransactionTag.INCOMING)
            }

            if (outgoingValue > incomingValue) {
                tags.add(TransactionTag.EVM_COIN_OUTGOING)
                tags.add(TransactionTag.OUTGOING)
            }

            return tags
        }

    private val tagsFromEventInstances: List<String>
        get() {
            val tags: MutableList<String> = mutableListOf()

            for (eventInstance in eventInstances) {
                tags.addAll(eventInstance.tags(userAddress))
            }

            return tags
        }

}