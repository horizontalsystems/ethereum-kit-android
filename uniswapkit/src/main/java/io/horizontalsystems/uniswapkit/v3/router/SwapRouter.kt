package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
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
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger,
        amountOut: BigInteger,
        tradeOptions: TradeOptions
    ): TransactionData {
        val recipient = tradeOptions.recipient ?: ethereumKit.receiveAddress
        val deadline = (Date().time / 1000 + tradeOptions.ttl).toBigInteger()
        val fee = BigInteger.valueOf(100)

        val ethValue = BigInteger.ZERO
        val method = when (tradeType) {
            TradeType.ExactIn -> {
                ExactInputSingleMethod(
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    fee = fee,
                    recipient = recipient,
                    deadline = deadline,
                    amountIn = amountIn,
                    amountOutMinimum = amountOut,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> {
                ExactOutputSingleMethod(
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    fee = fee,
                    recipient = recipient,
                    deadline = deadline,
                    amountOut = amountOut,
                    amountInMaximum = amountIn,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
        }

        return TransactionData(
            to = swapRouterAddress,
            value = ethValue,
            input = method.encodedABI()
        )
    }
}
