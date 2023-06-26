package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigInteger

class SwapRouter(private val ethereumKit: EthereumKit, dexType: DexType) {
    val swapRouterAddress = when (dexType) {
        DexType.Uniswap -> getUniswapRouterAddress(ethereumKit.chain)
        DexType.PancakeSwap -> getPancakeSwapRouterAddress(ethereumKit.chain)
    }

    private fun getUniswapRouterAddress(chain: Chain)= when (chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> Address("0x68b3465833fb72A70ecDF485E0e4C7bD8665Fc45")
        Chain.BinanceSmartChain -> Address("0xB971eF87ede563556b2ED4b1C0b0019111Dd85d2")
        else -> throw IllegalStateException("Not supported Uniswap chain ${ethereumKit.chain}")
    }

    private fun getPancakeSwapRouterAddress(chain: Chain)= when (chain) {
        Chain.BinanceSmartChain,
        Chain.Ethereum -> Address("0x13f4EA83D0bd40E75C8222255bc855a974568Dd4")
        else -> throw IllegalStateException("Not supported PancakeSwap chain ${ethereumKit.chain}")
    }

    fun transactionData(tradeData: TradeDataV3): TransactionData {
        val recipient = tradeData.options.recipient ?: ethereumKit.receiveAddress

        val swapRecipient = when {
            tradeData.tokenOut.isEther -> Address("0x0000000000000000000000000000000000000002")
            else -> recipient
        }

        val ethValue = when {
            tradeData.tokenIn.isEther -> tradeData.trade.amountIn
            else -> BigInteger.ZERO
        }

        val swapMethod = buildSwapMethod(tradeData, swapRecipient)

        val methods = buildList {
            add(swapMethod)

            when {
                tradeData.tokenIn.isEther && tradeData.tradeType == TradeType.ExactOut -> {
                    add(RefundETHMethod())
                }
                tradeData.tokenOut.isEther -> {
                    add(UnwrapWETH9Method(tradeData.amountOutMinimum, recipient))
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
        tradeData: TradeDataV3,
        swapRecipient: Address,
    ) = when {
        tradeData.singleSwap -> when (tradeData.tradeType) {
            TradeType.ExactIn -> {
                ExactInputSingleMethod(
                    tokenIn = tradeData.tokenIn.address,
                    tokenOut = tradeData.tokenOut.address,
                    fee = tradeData.singleSwapFee.value,
                    recipient = swapRecipient,
                    amountIn = tradeData.amountIn,
                    amountOutMinimum = tradeData.amountOutMinimum,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> {
                ExactOutputSingleMethod(
                    tokenIn = tradeData.tokenIn.address,
                    tokenOut = tradeData.tokenOut.address,
                    fee = tradeData.singleSwapFee.value,
                    recipient = swapRecipient,
                    amountOut = tradeData.amountOut,
                    amountInMaximum = tradeData.amountInMaximum,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
        }
        else -> when (tradeData.tradeType) {
            TradeType.ExactIn -> {
                ExactInputMethod(
                    path = tradeData.swapPath.abiEncodePacked(),
                    recipient = swapRecipient,
                    amountIn = tradeData.amountIn,
                    amountOutMinimum = tradeData.amountOutMinimum,
                )
            }
            TradeType.ExactOut -> {
                ExactOutputMethod(
                    path = tradeData.swapPath.abiEncodePacked(),
                    recipient = swapRecipient,
                    amountOut = tradeData.amountOut,
                    amountInMaximum = tradeData.amountInMaximum,
                )
            }
        }
    }
}
