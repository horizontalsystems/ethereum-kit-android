package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OneInchUnknownDecoration(
    override val contractAddress: Address,
    val tokenAmountIn: TokenAmount?,
    val tokenAmountOut: TokenAmount?,
) : OneInchDecoration(contractAddress) {

    class TokenAmount(val token: Token, val value: BigInteger)

    override fun tags() = buildList {
        addAll(super.tags())

        if (tokenAmountIn != null) {
            addAll(getTags(tokenAmountIn.token, TransactionTag.OUTGOING))
        }

        if (tokenAmountOut != null) {
            addAll(getTags(tokenAmountOut.token, TransactionTag.INCOMING))
        }
    }

}
