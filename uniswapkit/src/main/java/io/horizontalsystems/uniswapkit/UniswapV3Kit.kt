package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.v3.PriceImpactManager
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import io.horizontalsystems.uniswapkit.v3.UniswapV3MethodDecorator
import io.horizontalsystems.uniswapkit.v3.UniswapV3TransactionDecorator
import io.horizontalsystems.uniswapkit.v3.contract.UniswapV3ContractMethodFactories
import io.horizontalsystems.uniswapkit.v3.pool.PoolManager
import io.horizontalsystems.uniswapkit.v3.quoter.Quoter
import io.horizontalsystems.uniswapkit.v3.router.SwapRouter
import java.math.BigDecimal
import java.math.BigInteger

class UniswapV3Kit(
    private val quoter: Quoter,
    private val swapRouter: SwapRouter,
    private val tokenFactory: TokenFactory,
    private val priceImpactManager: PriceImpactManager
) {
    val routerAddress get() = swapRouter.swapRouterAddress

    fun etherToken(): Token {
        return tokenFactory.etherToken()
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return tokenFactory.token(contractAddress, decimals)
    }

    suspend fun bestTradeExactIn(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        tradeOptions: TradeOptions
    ) = bestTradeExactIn(
        tokenIn,
        tokenOut,
        amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
        tradeOptions
    )

    suspend fun bestTradeExactIn(
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        tradeOptions: TradeOptions
    ): TradeDataV3 {
        val bestTrade = quoter.bestTradeExactIn(tokenIn, tokenOut, amountIn)
        return TradeDataV3(
            bestTrade,
            tradeOptions,
            priceImpactManager.getPriceImpact(bestTrade)
        )
    }

    suspend fun bestTradeExactOut(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        tradeOptions: TradeOptions
    ) = bestTradeExactOut(
        tokenIn,
        tokenOut,
        amountOut.movePointRight(tokenOut.decimals).toBigInteger(),
        tradeOptions
    )

    suspend fun bestTradeExactOut(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger,
        tradeOptions: TradeOptions
    ): TradeDataV3 {
        val bestTrade = quoter.bestTradeExactOut(tokenIn, tokenOut, amountOut)
        return TradeDataV3(
            bestTrade,
            tradeOptions,
            priceImpactManager.getPriceImpact(bestTrade)
        )
    }

    fun transactionData(tradeData: TradeDataV3) = swapRouter.transactionData(tradeData)

    companion object {
        fun getInstance(ethereumKit: EthereumKit): UniswapV3Kit {
            val tokenFactory = TokenFactory(ethereumKit.chain)
            val quoter = Quoter(ethereumKit, tokenFactory.etherToken())
            val swapRouter = SwapRouter(ethereumKit)
            val poolManager = PoolManager(ethereumKit)
            val priceImpactManager = PriceImpactManager(poolManager)

            return UniswapV3Kit(quoter, swapRouter, tokenFactory, priceImpactManager)
        }

        fun addDecorators(ethereumKit: EthereumKit) {
            val tokenFactory = TokenFactory(ethereumKit.chain)
            ethereumKit.addMethodDecorator(UniswapV3MethodDecorator(UniswapV3ContractMethodFactories))
            ethereumKit.addTransactionDecorator(UniswapV3TransactionDecorator(tokenFactory.wethAddress))
        }

    }

}