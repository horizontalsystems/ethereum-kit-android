package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OneInchUnknownDecoration(
    override val contractAddress: Address,
    val tokenAmountIn: TokenAmount?,
    val tokenAmountOut: TokenAmount?
) : OneInchDecoration(contractAddress) {

    class TokenAmount(val token: Token, val value: BigInteger)

    override fun tags(): List<String> {
        val tags = super.tags().toMutableList()

        listOf(contractAddress.hex, TransactionTag.SWAP)

        if (tokenAmountIn != null) {
            tags.addAll(getTags(tokenAmountIn.token, TransactionTag.OUTGOING))
        }

        if (tokenAmountOut != null) {
            tags.addAll(getTags(tokenAmountOut.token, TransactionTag.INCOMING))
        }

        return tags
    }

}
