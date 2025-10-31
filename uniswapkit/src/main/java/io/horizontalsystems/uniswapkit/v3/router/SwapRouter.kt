package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import java.math.BigInteger

class SwapRouter(private val dexType: DexType) {

    fun swapRouterAddress(chain: Chain): Address = when (dexType) {
        DexType.Uniswap -> getUniswapRouterAddress(chain)
        DexType.PancakeSwap -> getPancakeSwapRouterAddress(chain)
    }

    private fun getUniswapRouterAddress(chain: Chain) = when (chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.BinanceSmartChain,
        Chain.Base -> Address("0x8f934fD34A92C1df0DbA4bEfAe7d16CCF255FeBD")

        Chain.ZkSync -> Address("0x99c56385daBCE3E81d8499d0b8d0257aBC07E8A3")
        Chain.EthereumGoerli -> Address("0x68b3465833fb72A70ecDF485E0e4C7bD8665Fc45")
        else -> throw IllegalStateException("Not supported Uniswap chain ${chain}")
    }

    private fun getPancakeSwapRouterAddress(chain: Chain) = when (chain) {
        Chain.BinanceSmartChain,
        Chain.Ethereum,
        Chain.Base -> Address("0x2a114a012A75A267b80a8a3c5FB26B32E86c32bA")
        Chain.ZkSync -> Address("0xf8b59f3c3Ab33200ec80a8A58b2aA5F5D2a8944C")

        else -> throw IllegalStateException("Not supported PancakeSwap chain ${chain}")
    }

    fun transactionData(
        receiveAddress: Address,
        chain: Chain,
        tradeData: TradeDataV3
    ): TransactionData {
        val recipient = tradeData.options.recipient ?: receiveAddress

        val swapRecipient = when {
            tradeData.tokenOut.isEther -> Address("0x0000000000000000000000000000000000000002")
            else -> recipient
        }

        val ethValue = when {
            tradeData.tokenIn.isEther -> when (tradeData.tradeType) {
                TradeType.ExactIn -> tradeData.amountIn
                TradeType.ExactOut -> tradeData.amountInMaximum
            }
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
            to = swapRouterAddress(chain),
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
