package io.horizontalsystems.uniswapkit.v3.SwapRouter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

class SwapRouter(private val ethereumKit: EthereumKit) {
    private val swapRouterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0xE592427A0AEce92De3Edee1F18E0157C05861564"
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    fun transactionData(
        tradeType: TradeType,
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigDecimal,
        amountOut: BigDecimal,
        tradeOptions: TradeOptions
    ): TransactionData {
        val recipient = tradeOptions.recipient ?: ethereumKit.receiveAddress
        val deadline = (Date().time / 1000 + tradeOptions.ttl).toBigInteger()

        return when (tradeType) {
            TradeType.ExactIn -> {
                transactionDataExactIn(
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    recipient = recipient,
                    deadline = deadline,
                    amountIn = amountIn,
                    amountOutMinimum = amountOut,
                    sqrtPriceLimitX96 = BigInteger.ZERO
                )
            }
            TradeType.ExactOut -> TODO()
        }
    }

    private fun transactionDataExactIn(
        tokenIn: Address,
        tokenOut: Address,
        recipient: Address,
        deadline: BigInteger,
        amountIn: BigDecimal,
        amountOutMinimum: BigDecimal,
        sqrtPriceLimitX96: BigInteger,
    ): TransactionData {
        val ethValue = BigInteger.ZERO

        return TransactionData(
            to = Address(swapRouterAddress),
            value = ethValue,
            input = ExactInputSingleMethod().encodedABI()
        )
    }
}
