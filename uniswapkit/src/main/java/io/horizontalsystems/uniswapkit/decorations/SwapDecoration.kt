package io.horizontalsystems.uniswapkit.decorations

import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class SwapDecoration(
        val contractAddress: Address,
        val amountIn: Amount,
        val amountOut: Amount,
        val tokenIn: Token,
        val tokenOut: Token,
        val recipient: Address?,
        val deadline: BigInteger?
) : TransactionDecoration() {

    sealed class Amount(val value: BigInteger) {
        class Exact(value: BigInteger) : Amount(value)
        class Extremum(value: BigInteger) : Amount(value)
    }

    sealed class Token {
        object EvmCoin : Token()
        class Eip20Coin(val address: Address, val tokenInfo: TokenInfo? = null) : Token()

        val info: TokenInfo?
            get() = when (this) {
                is Eip20Coin -> tokenInfo
                EvmCoin -> null
            }
    }

    override fun tags(): List<String> {
        val tags: MutableList<String> = mutableListOf(contractAddress.hex, "swap")

        tags.addAll(tags(tokenIn, "outgoing"))

        if (recipient == null) {
            tags.addAll(tags(tokenOut, "incoming"))
        }

        return tags
    }

    private fun tags(token: Token, type: String): List<String> =
            when (token) {
                is Token.EvmCoin -> listOf("${TransactionTag.EVM_COIN}_$type", TransactionTag.EVM_COIN, type)
                is Token.Eip20Coin -> listOf("${token.address.hex}_$type", token.address.hex, type)
            }

}
