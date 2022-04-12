package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
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

    override fun tags(): List<String> {
        val tags = super.tags().toMutableList()

        listOf(contractAddress.hex, "swap")

        tags.addAll(getTags(tokenIn, "outgoing"))

        if (recipient == null) {
            tags.addAll(getTags(tokenOut, "incoming"))
        }

        return tags
    }

}
