package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class OneInchUnoswapDecoration(
    override val contractAddress: Address,
    val tokenIn: Token,
    val tokenOut: Token?,
    val amountIn: BigInteger,
    val amountOut: Amount,
    val params: Bytes32Array
) : OneInchDecoration(contractAddress) {

    override fun tags(): List<String> {
        val tags = super.tags().toMutableList()

        listOf(contractAddress.hex, "swap")

        tags.addAll(getTags(tokenIn, "outgoing"))

        if (tokenOut != null) {
            tags.addAll(getTags(tokenOut, "incoming"))
        }

        return tags
    }

}
