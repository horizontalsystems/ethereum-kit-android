package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

open class UnknownTransactionDecoration(
    private val userAddress: Address,
    private val value: BigInteger?,
    open val internalTransactions: List<InternalTransaction>,
    open val eventInstances: List<ContractEventInstance>
) : TransactionDecoration() {

    override fun tags(): List<String> =
        (tagsFromInternalTransactions + tagsFromEventInstances).toSet().toList()

    private val tagsFromInternalTransactions: List<String>
        get() {
            val incomingInternalTransactions = internalTransactions.filter { it.to == userAddress }
            if (incomingInternalTransactions.isEmpty()) return listOf()

            var totalAmount: BigInteger = BigInteger.ZERO

            for (internalTx in incomingInternalTransactions) {
                totalAmount += internalTx.value
            }

            if (totalAmount > (value ?: BigInteger.ZERO))
                return listOf(TransactionTag.EVM_COIN_OUTGOING, TransactionTag.EVM_COIN, TransactionTag.OUTGOING)

            return listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.EVM_COIN, TransactionTag.INCOMING)
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