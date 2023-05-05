package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import io.horizontalsystems.uniswapkit.v3.SwapPath
import io.horizontalsystems.uniswapkit.v3.SwapPathItem
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.rx2.await
import java.math.BigInteger
import kotlin.coroutines.coroutineContext

class Quoter(private val ethereumKit: EthereumKit, private val weth: Token) {

    private val quoterAddress = when (ethereumKit.chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0xb27308f9F90D607463bb33eA1BeBb41C27CE5AB6"
        else -> throw IllegalStateException("Not supported chain ${ethereumKit.chain}")
    }

    suspend fun bestTradeExactIn(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade {
        quoteExactInputSingle(tokenIn, tokenOut, amountIn)?.let {
            return it
        }
        quoteExactInputMultihop(tokenIn, tokenOut, amountIn)?.let {
            return it
        }
        throw TradeError.TradeNotFound()
    }

    private suspend fun quoteExactInputSingle(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    contractAddress = Address(quoterAddress),
                    data = QuoteExactInputSingleMethod(
                        tokenIn = tokenIn.address,
                        tokenOut = tokenOut.address,
                        fee = fee.value,
                        amountIn = amountIn,
                        sqrtPriceLimitX96 = sqrtPriceLimitX96
                    ).encodedABI()
                )

                val amountOut = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTrade(
                    tradeType = TradeType.ExactIn,
                    swapPath = SwapPath(listOf(SwapPathItem(tokenIn.address, tokenOut.address, fee))),
                    amountIn = amountIn,
                    amountOut = amountOut,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut
                )
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.maxByOrNull { it.amountOut }
    }

    private suspend fun quoteExactInputMultihop(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade? {

        val swapInToWeth = quoteExactInputSingle(
            tokenIn = tokenIn,
            tokenOut = weth,
            amountIn = amountIn
        ) ?: return null

        val swapWethToOut = quoteExactInputSingle(
            tokenIn = weth,
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
            BestTrade(
                tradeType = TradeType.ExactIn,
                swapPath = path,
                amountIn = amountIn,
                amountOut = amountOut,
                tokenIn = tokenIn,
                tokenOut = tokenOut
            )
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun bestTradeExactOut(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade {
        quoteExactOutputSingle(tokenIn, tokenOut, amountOut)?.let {
            return it
        }
        quoteExactOutputMultihop(tokenIn, tokenOut, amountOut)?.let {
            return it
        }
        throw TradeError.TradeNotFound()
    }

    private suspend fun quoteExactOutputSingle(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = FeeAmount.sorted().mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    contractAddress = Address(quoterAddress),
                    data = QuoteExactOutputSingleMethod(
                        tokenIn = tokenIn.address,
                        tokenOut = tokenOut.address,
                        fee = fee.value,
                        amountOut = amountOut,
                        sqrtPriceLimitX96 = sqrtPriceLimitX96
                    ).encodedABI()
                )

                val amountIn = callResponse.sliceArray(IntRange(0, 31)).toBigInteger()
                BestTrade(
                    tradeType = TradeType.ExactOut,
                    swapPath = SwapPath(listOf(SwapPathItem(tokenOut.address, tokenIn.address, fee))),
                    amountIn = amountIn,
                    amountOut = amountOut,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut
                )
            } catch (t: Throwable) {
                null
            }
        }

        return amounts.minByOrNull { it.amountIn }
    }

    private suspend fun quoteExactOutputMultihop(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade? {
        val swapWethToOut = quoteExactOutputSingle(
            tokenIn = weth,
            tokenOut = tokenOut,
            amountOut = amountOut
        ) ?: return null

        val swapInToWeth = quoteExactOutputSingle(
            tokenIn = tokenIn,
            tokenOut = weth,
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
            BestTrade(
                tradeType = TradeType.ExactOut,
                swapPath = path,
                amountIn = amountIn,
                amountOut = amountOut,
                tokenIn = tokenIn,
                tokenOut = tokenOut
            )
        } catch (t: Throwable) {
            null
        }
    }

    private suspend fun ethCall(contractAddress: Address, data: ByteArray): ByteArray {
        return ethereumKit.call(contractAddress, data).await()
    }
}
