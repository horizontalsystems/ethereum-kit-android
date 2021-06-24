package io.horizontalsystems.uniswapkit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapMethodDecoration(
        val trade: Trade, val tokenIn: Token, val tokenOut: Token, val to: Address, val deadline: BigInteger
) : ContractMethodDecoration() {

    sealed class Trade {
        class ExactIn(val amountIn: BigInteger, val amountOutMin: BigInteger, val amountOut: BigInteger? = null) : Trade()
        class ExactOut(val amountOut: BigInteger, val amountInMax: BigInteger, val amountIn: BigInteger? = null) : Trade()
    }

    sealed class Token {
        object EvmCoin : Token()
        class Eip20Coin(val address: Address) : Token()
    }

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(toAddress.hex, "swap")

        when (tokenIn) {
            Token.EvmCoin -> tags.addAll(listOf("ETH_outgoing", "ETH", "outgoing"))
            is Token.Eip20Coin -> tags.addAll(listOf("${tokenIn.address.hex}_outgoing", tokenIn.address.hex, "outgoing"))
        }

        if (to == userAddress) {
            when (tokenOut) {
                Token.EvmCoin -> tags.addAll(listOf("ETH_incoming", "ETH", "incoming"))
                is Token.Eip20Coin -> tags.addAll(listOf("${tokenOut.address.hex}_incoming", tokenOut.address.hex, "incoming"))
            }
        }

        return tags
    }
}
