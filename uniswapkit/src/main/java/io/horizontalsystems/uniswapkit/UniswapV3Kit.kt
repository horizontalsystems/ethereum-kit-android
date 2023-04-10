package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.models.TradeType
import io.horizontalsystems.uniswapkit.v3.Quoter.Quoter
import io.horizontalsystems.uniswapkit.v3.SwapRouter.SwapRouter
import java.math.BigInteger

class UniswapV3Kit(
    private val quoter: Quoter,
    private val swapRouter: SwapRouter
) {
    val routerAddress get() = swapRouter.swapRouterAddress

    fun bestTradeExactIn(
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger
    ) = quoter.bestTradeExactIn(tokenIn, tokenOut, amountIn)

    fun bestTradeExactOut(
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger
    ) = quoter.bestTradeExactOut(tokenIn, tokenOut, amountOut)

    fun transactionData(
        tradeType: TradeType,
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger,
        amountOut: BigInteger,
        tradeOptions: TradeOptions
    ) = swapRouter.transactionData(
        tradeType,
        tokenIn,
        tokenOut,
        amountIn,
        amountOut,
        tradeOptions
    )

    companion object {
        fun getInstance(ethereumKit: EthereumKit): UniswapV3Kit {
            return UniswapV3Kit(Quoter(ethereumKit), SwapRouter(ethereumKit))
        }

//        fun addDecorators(ethereumKit: EthereumKit) {
//            ethereumKit.addMethodDecorator(SwapMethodDecorator(SwapContractMethodFactories))
//            ethereumKit.addTransactionDecorator(SwapTransactionDecorator())
//        }

    }

}