package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.uniswapkit.TokenFactory
import io.horizontalsystems.uniswapkit.TradeError
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.FeeAmount
import io.horizontalsystems.uniswapkit.v3.SwapPath
import io.horizontalsystems.uniswapkit.v3.SwapPathItem
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.rx2.await
import java.math.BigInteger
import kotlin.coroutines.coroutineContext

class QuoterV2(
    private val tokenFactory: TokenFactory,
    private val dexType: DexType
) {

    private val feeAmounts = FeeAmount.sorted(dexType)

    private fun quoterAddress(chain: Chain) = when (dexType) {
        DexType.Uniswap -> getUniswapQuoterAddress(chain)
        DexType.PancakeSwap -> getPancakeSwapQuoterAddress(chain)
    }

    private fun getUniswapQuoterAddress(chain: Chain) = when (chain) {
        Chain.Ethereum,
        Chain.Polygon,
        Chain.Optimism,
        Chain.ArbitrumOne,
        Chain.EthereumGoerli -> "0x61fFE014bA17989E743c5F6cB21bF9697530B21e"

        Chain.BinanceSmartChain -> "0x78D78E420Da98ad378D7799bE8f4AF69033EB077"
        Chain.Base -> "0x3d4e44Eb1374240CE5F1B871ab261CD16335B76a"
        Chain.ZkSync -> "0x8Cb537fc92E26d8EBBb760E632c95484b6Ea3e28"
        else -> throw IllegalStateException("Not supported Uniswap chain $chain")
    }

    private fun getPancakeSwapQuoterAddress(chain: Chain) = when (chain) {
        Chain.BinanceSmartChain,
        Chain.Ethereum -> "0xB048Bbc1Ee6b733FFfCFb9e9CeF7375518e25997"
        Chain.ZkSync -> "0x3d146FcE6c1006857750cBe8aF44f76a28041CCc"

        else -> throw IllegalStateException("Not supported PancakeSwap chain $chain")
    }

    suspend fun bestTradeExactIn(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade {
        quoteExactInputSingle(rpcSource, chain, tokenIn, tokenOut, amountIn)?.let {
            return it
        }
        quoteExactInputMultihop(rpcSource, chain, tokenIn, tokenOut, amountIn)?.let {
            return it
        }
        throw TradeError.TradeNotFound()
    }

    private suspend fun quoteExactInputSingle(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = feeAmounts.mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    rpcSource = rpcSource,
                    chain = chain,
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
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger
    ): BestTrade? {
        val weth = tokenFactory.etherToken(chain)

        val swapInToWeth = quoteExactInputSingle(
            rpcSource = rpcSource,
            chain = chain,
            tokenIn = tokenIn,
            tokenOut = weth,
            amountIn = amountIn
        ) ?: return null

        val swapWethToOut = quoteExactInputSingle(
            rpcSource = rpcSource,
            chain = chain,
            tokenIn = weth,
            tokenOut = tokenOut,
            amountIn = swapInToWeth.amountOut
        ) ?: return null

        val path = SwapPath(swapInToWeth.swapPath.items + swapWethToOut.swapPath.items)

        coroutineContext.ensureActive()
        return try {
            val callResponse = ethCall(
                rpcSource = rpcSource,
                chain = chain,
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
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade {
        quoteExactOutputSingle(rpcSource, chain, tokenIn, tokenOut, amountOut)?.let {
            return it
        }
        quoteExactOutputMultihop(rpcSource, chain, tokenIn, tokenOut, amountOut)?.let {
            return it
        }
        throw TradeError.TradeNotFound()
    }

    private suspend fun quoteExactOutputSingle(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade? {
        val sqrtPriceLimitX96 = BigInteger.ZERO

        val amounts = feeAmounts.mapNotNull { fee ->
            coroutineContext.ensureActive()
            try {
                val callResponse = ethCall(
                    rpcSource = rpcSource,
                    chain = chain,
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
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ): BestTrade? {
        val weth = tokenFactory.etherToken(chain)

        val swapWethToOut = quoteExactOutputSingle(
            rpcSource = rpcSource,
            chain = chain,
            tokenIn = weth,
            tokenOut = tokenOut,
            amountOut = amountOut
        ) ?: return null

        val swapInToWeth = quoteExactOutputSingle(
            rpcSource = rpcSource,
            chain = chain,
            tokenIn = tokenIn,
            tokenOut = weth,
            amountOut = swapWethToOut.amountIn
        ) ?: return null

        val path = SwapPath(swapWethToOut.swapPath.items + swapInToWeth.swapPath.items)

        coroutineContext.ensureActive()
        return try {
            val callResponse = ethCall(
                rpcSource = rpcSource,
                chain = chain,
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

    private suspend fun ethCall(rpcSource: RpcSource, chain: Chain, data: ByteArray): ByteArray {
        val quoterAddress = Address(quoterAddress(chain))
        return EthereumKit.call(rpcSource, quoterAddress, data).await()
    }
}
