package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import java.math.BigInteger

class OneInchUnknownDecoration(
    val contractAddress: Address,
    private val userAddress: Address,
    val value: BigInteger,
    override val internalTransactions: List<InternalTransaction>,
    override val eventInstances: List<ContractEventInstance>
) : UnknownTransactionDecoration(userAddress, value, internalTransactions, eventInstances) {

    override fun tags(): List<String> {
        val tags = super.tags().toMutableList()

        tags.add(contractAddress.hex)
        tags.add("swap")

        return tags
    }

}
