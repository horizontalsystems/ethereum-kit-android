package io.horizontalsystems.uniswapkit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
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
        val tags = mutableListOf(toAddress.hex, TransactionTag.SWAP)

        when (tokenIn) {
            Token.EvmCoin -> tags.addAll(listOf(TransactionTag.EVM_COIN_OUTGOING, TransactionTag.EVM_COIN, TransactionTag.OUTGOING))
            is Token.Eip20Coin -> tags.addAll(listOf(TransactionTag.eip20Outgoing(tokenIn.address.hex), tokenIn.address.hex, TransactionTag.OUTGOING))
        }

        if (to == userAddress) {
            when (tokenOut) {
                Token.EvmCoin -> tags.addAll(listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.EVM_COIN, TransactionTag.INCOMING))
                is Token.Eip20Coin -> tags.addAll(listOf(TransactionTag.eip20Incoming(tokenOut.address.hex), tokenOut.address.hex, TransactionTag.INCOMING))
            }
        }

        return tags
    }
}
