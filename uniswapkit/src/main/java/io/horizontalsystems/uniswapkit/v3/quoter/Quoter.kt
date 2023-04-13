package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.TokenFactory
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import io.horizontalsystems.uniswapkit.v3.SwapPath
import io.horizontalsystems.uniswapkit.v3.SwapPathItem
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.rx2.await
import java.math.BigInteger
import kotlin.coroutines.coroutineContext

class Quoter(private val ethereumKit: EthereumKit) {
    private val tokenFactory = TokenFactory(ethereumKit.chain)

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
    ): BestTradeExactIn? {
        quoteExactInputSingle(tokenIn, tokenOut, amountIn)?.let {
            return it
        }
        quoteExactInputMultihop(tokenIn, tokenOut, amountIn)?.let {
            return it
        }

        return null
    }

    private suspend fun quoteExactInputSingle(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ): BestTradeExactIn? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    contractAddress = Address(quoterAddress),
                    data = QuoteExactInputSingleMethod(
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        fee = fee.value,
                        amountIn = amountIn,
                        sqrtPriceLimitX96 = sqrtPriceLimitX96
                    ).encodedABI()
                )

                val amountOut = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTradeExactIn(SwapPath(listOf(SwapPathItem(tokenIn, tokenOut, fee))), amountOut)
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.maxByOrNull { it.amountOut }
    }

    private suspend fun quoteExactInputMultihop(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ): BestTradeExactIn? {
        val weth = tokenFactory.etherToken()

        val swapInToWeth = quoteExactInputSingle(
            tokenIn = tokenIn,
            tokenOut = weth.address,
            amountIn = amountIn
        ) ?: return null

        val swapWethToOut = quoteExactInputSingle(
            tokenIn = weth.address,
            tokenOut = tokenOut,
            amountIn = swapInToWeth.amountOut
        ) ?: return null

        val path = SwapPath(swapInToWeth.swapPath.items + swapWethToOut.swapPath.items)

        coroutineContext.ensureActive()
        return try {
            val callResponse = ethCall(
                contractAddress = Address(quoterAddress),
                data = QuoteExactInputMethod(
                    path = path,
                    amountIn = amountIn,
                ).encodedABI()
            )

            val amountOut = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
            BestTradeExactIn(path, amountOut)
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun bestTradeExactOut(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): BestTradeExactOut? {
        return quoteExactOutputSingle(tokenIn, tokenOut, amountOut)
            ?: quoteExactOutputMultihop(tokenIn, tokenOut, amountOut)
    }

    private suspend fun quoteExactOutputSingle(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): BestTradeExactOut? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    contractAddress = Address(quoterAddress),
                    data = QuoteExactOutputSingleMethod(
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        fee = fee.value,
                        amountOut = amountOut,
                        sqrtPriceLimitX96 = sqrtPriceLimitX96
                    ).encodedABI()
                )

                val amountIn = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTradeExactOut(SwapPath(listOf(SwapPathItem(tokenOut, tokenIn, fee))), amountIn)
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.minByOrNull { it.amountIn }
    }

    private suspend fun quoteExactOutputMultihop(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ): BestTradeExactOut? {
        val weth = tokenFactory.etherToken()

        val swapWethToOut = quoteExactOutputSingle(
            tokenIn = weth.address,
            tokenOut = tokenOut,
            amountOut = amountOut
        ) ?: return null

        val swapInToWeth = quoteExactOutputSingle(
            tokenIn = tokenIn,
            tokenOut = weth.address,
            amountOut = swapWethToOut.amountIn
        ) ?: return null

        val path = SwapPath(swapWethToOut.swapPath.items + swapInToWeth.swapPath.items)

        coroutineContext.ensureActive()
        return try {
            val callResponse = ethCall(
                contractAddress = Address(quoterAddress),
                data = QuoteExactOutputMethod(
                    path = path,
                    amountOut = amountOut,
                ).encodedABI()
            )

            val amountIn = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
            BestTradeExactOut(path, amountIn)
        } catch (t: Throwable) {
            null
        }
    }

    private suspend fun ethCall(contractAddress: Address, data: ByteArray): ByteArray {
        return ethereumKit.call(contractAddress, data).await()
    }
}
