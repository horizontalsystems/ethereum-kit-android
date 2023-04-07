package io.horizontalsystems.uniswapkit.v3.Quoter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
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

    fun bestTradeExactIn(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ): Single<BigInteger> {
        val fee = BigInteger.valueOf(100)
        val sqrtPriceLimitX96 = BigInteger.ZERO

        return ethereumKit.call(
            contractAddress = Address(quoterAddress),
            data = QuoteExactInputSingleMethod(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                fee = fee,
                amountIn = amountIn,
                sqrtPriceLimitX96 = sqrtPriceLimitX96
            ).encodedABI(),
        ).map {
            it.sliceArray(IntRange(0, 31)).toBigInteger()
        }
    }

    fun bestTradeExactOut(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): Single<BigInteger> {
        val fee = BigInteger.valueOf(100)
        val sqrtPriceLimitX96 = BigInteger.ZERO

        return ethereumKit.call(
            contractAddress = Address(quoterAddress),
            data = QuoteExactOutputSingleMethod(
                tokenIn = tokenIn,
                tokenOut = tokenOut,
                fee = fee,
                amountOut = amountOut,
                sqrtPriceLimitX96 = sqrtPriceLimitX96
            ).encodedABI(),
        ).map {
            it.sliceArray(IntRange(0, 31)).toBigInteger()
        }
    }
}
