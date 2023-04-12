package io.horizontalsystems.uniswapkit.v3.Quoter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import kotlinx.coroutines.rx2.await
import java.math.BigInteger

class Quoter(private val ethereumKit: EthereumKit) {
    private val quoterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0xb27308f9F90D607463bb33eA1BeBb41C27CE5AB6"
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    suspend fun bestTradeExactIn(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ): BestTradeExactIn {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            try {
                val callResponse = ethereumKit
                    .call(
                        contractAddress = Address(quoterAddress),
                        data = QuoteExactInputSingleMethod(
                            tokenIn = tokenIn,
                            tokenOut = tokenOut,
                            fee = fee.value,
                            amountIn = amountIn,
                            sqrtPriceLimitX96 = sqrtPriceLimitX96
                        ).encodedABI(),
                    )
                    .await()

                val amountOut = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTradeExactIn(fee, amountOut)
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.maxBy { it.amountOut }
    }

    suspend fun bestTradeExactOut(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): BestTradeExactOut {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            try {
                val callResponse = ethereumKit
                    .call(
                        contractAddress = Address(quoterAddress),
                        data = QuoteExactOutputSingleMethod(
                            tokenIn = tokenIn,
                            tokenOut = tokenOut,
                            fee = fee.value,
                            amountOut = amountOut,
                            sqrtPriceLimitX96 = sqrtPriceLimitX96
                        ).encodedABI(),
                    )
                    .await()

                val amountIn = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTradeExactOut(fee, amountIn)
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.minBy { it.amountIn }
    }
}

data class BestTradeExactIn(val fee: FeeAmount, val amountOut: BigInteger)
data class BestTradeExactOut(val fee: FeeAmount, val amountIn: BigInteger)
