package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OneInchSwapDecoration(
    override val contractAddress: Address,
    val tokenIn: Token,
    val tokenOut: Token,
    val amountIn: BigInteger,
    val amountOut: Amount,
    val flags: BigInteger,
    val permit: ByteArray,
    val data: ByteArray,
    val recipient: Address?
) : OneInchDecoration(contractAddress) {

    override fun tags() = buildList {
        addAll(super.tags())
        addAll(getTags(tokenIn, TransactionTag.OUTGOING))

        if (recipient == null) {
            addAll(getTags(tokenOut, TransactionTag.INCOMING))
        } else {
            add(TransactionTag.toAddress(recipient.hex))
        }
    }

}
