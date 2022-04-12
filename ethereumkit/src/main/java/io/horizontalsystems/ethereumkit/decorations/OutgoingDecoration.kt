package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OutgoingDecoration(
    val to: Address,
    val value: BigInteger,
    val sentToSelf: Boolean
) : TransactionDecoration() {

    override fun tags(): List<String> {
        val tags = mutableListOf(TransactionTag.EVM_COIN, TransactionTag.EVM_COIN_OUTGOING, TransactionTag.OUTGOING)

        if (sentToSelf) {
            tags += listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.INCOMING)
        }

        return tags
    }

}
