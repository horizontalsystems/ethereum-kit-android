package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class IncomingDecoration(
    val from: Address,
    val value: BigInteger
) : TransactionDecoration() {

    override fun tags(): List<String> =
        listOf(TransactionTag.EVM_COIN, TransactionTag.EVM_COIN_INCOMING, TransactionTag.INCOMING)

}
