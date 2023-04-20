package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.v3.quoter.BestTrade
import io.horizontalsystems.uniswapkit.v3.quoter.Quoter
import io.horizontalsystems.uniswapkit.v3.router.SwapRouter
import java.math.BigInteger

class UniswapV3Kit(
    private val quoter: Quoter,
    private val swapRouter: SwapRouter,
    private val tokenFactory: TokenFactory
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
        amountIn: BigInteger
    ) = quoter.bestTradeExactIn(tokenIn, tokenOut, amountIn)

    suspend fun bestTradeExactOut(
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger
    ) = quoter.bestTradeExactOut(tokenIn, tokenOut, amountOut)

    fun transactionData(
        bestTrade: BestTrade,
        tradeOptions: TradeOptions
    ) = swapRouter.transactionData(
        bestTrade,
        tradeOptions
    )

    companion object {
        fun getInstance(ethereumKit: EthereumKit): UniswapV3Kit {
            val tokenFactory = TokenFactory(ethereumKit.chain)
            val quoter = Quoter(ethereumKit, tokenFactory.etherToken())
            val swapRouter = SwapRouter(ethereumKit)

            return UniswapV3Kit(quoter, swapRouter, tokenFactory)
        }

//        fun addDecorators(ethereumKit: EthereumKit) {
//            ethereumKit.addMethodDecorator(SwapMethodDecorator(SwapContractMethodFactories))
//            ethereumKit.addTransactionDecorator(SwapTransactionDecorator())
//        }

    }

}