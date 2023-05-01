package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.quoter.BestTrade
import java.math.BigInteger

class SwapRouter(private val ethereumKit: EthereumKit) {
    val swapRouterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> Address("0x68b3465833fb72A70ecDF485E0e4C7bD8665Fc45")
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    fun transactionData(
        bestTrade: BestTrade,
        tradeOptions: TradeOptions
    ): TransactionData {
        val recipient = tradeOptions.recipient ?: ethereumKit.receiveAddress

        val swapRecipient = when {
            bestTrade.tokenOut.isEther -> Address("0x0000000000000000000000000000000000000000")
            else -> recipient
        }

        val ethValue = when {
            bestTrade.tokenIn.isEther -> bestTrade.amountIn
            else -> BigInteger.ZERO
        }

        val swapMethod = buildSwapMethod(bestTrade, swapRecipient)

        val methods = buildList {
            add(swapMethod)

            when {
                bestTrade.tokenIn.isEther && bestTrade.tradeType == TradeType.ExactOut -> {
                    add(RefundETHMethod())
                }
                bestTrade.tokenOut.isEther -> {
                    add(UnwrapWETH9Method(bestTrade.amountOut, recipient))
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
        bestTrade: BestTrade,
        swapRecipient: Address,
    ) = when {
        bestTrade.singleSwap -> when (bestTrade.tradeType) {
            TradeType.ExactIn -> {
                ExactInputSingleMethod(
                    tokenIn = bestTrade.tokenIn.address,
                    tokenOut = bestTrade.tokenOut.address,
                    fee = bestTrade.singleSwapFee.value,
                    recipient = swapRecipient,
                    amountIn = bestTrade.amountIn,
                    amountOutMinimum = bestTrade.amountOut,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> {
                ExactOutputSingleMethod(
                    tokenIn = bestTrade.tokenIn.address,
                    tokenOut = bestTrade.tokenOut.address,
                    fee = bestTrade.singleSwapFee.value,
                    recipient = swapRecipient,
                    amountOut = bestTrade.amountOut,
                    amountInMaximum = bestTrade.amountIn,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
        }
        else -> when (bestTrade.tradeType) {
            TradeType.ExactIn -> {
                ExactInputMethod(
                    path = bestTrade.swapPath.abiEncodePacked(),
                    recipient = swapRecipient,
                    amountIn = bestTrade.amountIn,
                    amountOutMinimum = bestTrade.amountOut,
                )
            }
            TradeType.ExactOut -> {
                ExactOutputMethod(
                    path = bestTrade.swapPath.abiEncodePacked(),
                    recipient = swapRecipient,
                    amountOut = bestTrade.amountOut,
                    amountInMaximum = bestTrade.amountIn,
                )
            }
        }
    }
}
