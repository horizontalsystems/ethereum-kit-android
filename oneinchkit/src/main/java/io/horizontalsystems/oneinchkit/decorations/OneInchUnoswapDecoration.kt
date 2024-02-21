package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OneInchUnoswapDecoration(
    override val contractAddress: Address,
    val tokenIn: Token,
    val tokenOut: Token?,
    val amountIn: BigInteger,
    val amountOut: Amount,
    val params: Bytes32Array
) : OneInchDecoration(contractAddress) {

    override fun tags() = buildList {
        addAll(super.tags())
        addAll(getTags(tokenIn, TransactionTag.OUTGOING))

        if (tokenOut != null) {
            addAll(getTags(tokenOut, TransactionTag.INCOMING))
        }
    }
}
