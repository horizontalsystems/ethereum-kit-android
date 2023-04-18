package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger
import java.util.*

class SwapRouter(private val ethereumKit: EthereumKit) {
    val swapRouterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> Address("0xE592427A0AEce92De3Edee1F18E0157C05861564")
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    fun transactionData(
        tradeType: TradeType,
        swapPath: SwapPath,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        amountOut: BigInteger,
        tradeOptions: TradeOptions
    ): TransactionData {
        val recipient = tradeOptions.recipient ?: ethereumKit.receiveAddress
        val deadline = (Date().time / 1000 + tradeOptions.ttl).toBigInteger()

        val swapRecipient = when {
            tokenOut.isEther -> Address("0x0000000000000000000000000000000000000000")
            else -> recipient
        }

        val ethValue = when {
            tokenIn.isEther -> amountIn
            else -> BigInteger.ZERO
        }

        val swapMethod = buildSwapMethod(
            tradeType,
            swapPath,
            tokenIn,
            tokenOut,
            amountIn,
            amountOut,
            swapRecipient,
            deadline
        )

        val methods = buildList {
            add(swapMethod)

            when {
                tokenIn.isEther && tradeType == TradeType.ExactOut -> {
                    add(RefundETHMethod())
                }
                tokenOut.isEther -> {
                    add(UnwrapWETH9Method(amountOut, recipient))
                }
            }
        }

        val method = methods.singleOrNull() ?: MulticallMethod(methods)

        return TransactionData(
            to = swapRouterAddress,
            value = ethValue,
            input = method.encodedABI()
        )
    }

    private fun buildSwapMethod(
        tradeType: TradeType,
        swapPath: SwapPath,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        amountOut: BigInteger,
        swapRecipient: Address,
        deadline: BigInteger
    ) = when {
        swapPath.singleSwap -> when (tradeType) {
            TradeType.ExactIn -> {
                ExactInputSingleMethod(
                    tokenIn = tokenIn.address,
                    tokenOut = tokenOut.address,
                    fee = swapPath.singleSwapFee.value,
                    recipient = swapRecipient,
                    deadline = deadline,
                    amountIn = amountIn,
                    amountOutMinimum = amountOut,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> {
                ExactOutputSingleMethod(
                    tokenIn = tokenIn.address,
                    tokenOut = tokenOut.address,
                    fee = swapPath.singleSwapFee.value,
                    recipient = swapRecipient,
                    deadline = deadline,
                    amountOut = amountOut,
                    amountInMaximum = amountIn,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
        }
        else -> when (tradeType) {
            TradeType.ExactIn -> {
                ExactInputMethod(
                    path = swapPath,
                    recipient = swapRecipient,
                    deadline = deadline,
                    amountIn = amountIn,
                    amountOutMinimum = amountOut,
                )
            }
            TradeType.ExactOut -> {
                ExactOutputMethod(
                    path = swapPath,
                    recipient = swapRecipient,
                    deadline = deadline,
                    amountOut = amountOut,
                    amountInMaximum = amountIn,
                )
            }
        }
    }
}
