package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.uniswapkit.models.DexType
import io.horizontalsystems.uniswapkit.models.Token
import io.horizontalsystems.uniswapkit.models.TradeOptions
import io.horizontalsystems.uniswapkit.v3.PriceImpactManager
import io.horizontalsystems.uniswapkit.v3.TradeDataV3
import io.horizontalsystems.uniswapkit.v3.UniswapV3MethodDecorator
import io.horizontalsystems.uniswapkit.v3.UniswapV3TransactionDecorator
import io.horizontalsystems.uniswapkit.v3.contract.UniswapV3ContractMethodFactories
import io.horizontalsystems.uniswapkit.v3.pool.PoolManager
import io.horizontalsystems.uniswapkit.v3.quoter.QuoterV2
import io.horizontalsystems.uniswapkit.v3.router.SwapRouter
import java.math.BigDecimal
import java.math.BigInteger

class UniswapV3Kit(
    private val quoter: QuoterV2,
    private val swapRouter: SwapRouter,
    private val tokenFactory: TokenFactory,
    private val priceImpactManager: PriceImpactManager
) {
    fun routerAddress(chain: Chain): Address = swapRouter.swapRouterAddress(chain)

    fun etherToken(chain: Chain): Token {
        return tokenFactory.etherToken(chain)
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return tokenFactory.token(contractAddress, decimals)
    }

    suspend fun bestTradeExactIn(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigDecimal,
        tradeOptions: TradeOptions
    ) = bestTradeExactIn(
        rpcSource,
        chain,
        tokenIn,
        tokenOut,
        amountIn.movePointRight(tokenIn.decimals).toBigInteger(),
        tradeOptions
    )

    suspend fun bestTradeExactIn(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountIn: BigInteger,
        tradeOptions: TradeOptions
    ): TradeDataV3 {
        val bestTrade = quoter.bestTradeExactIn(rpcSource, chain, tokenIn, tokenOut, amountIn)
        return TradeDataV3(
            bestTrade,
            tradeOptions,
            priceImpactManager.getPriceImpact(rpcSource, chain, bestTrade)
        )
    }

    suspend fun bestTradeExactOut(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigDecimal,
        tradeOptions: TradeOptions
    ) = bestTradeExactOut(
        rpcSource,
        chain,
        tokenIn,
        tokenOut,
        amountOut.movePointRight(tokenOut.decimals).toBigInteger(),
        tradeOptions
    )

    suspend fun bestTradeExactOut(
        rpcSource: RpcSource,
        chain: Chain,
        tokenIn: Token,
        tokenOut: Token,
        amountOut: BigInteger,
        tradeOptions: TradeOptions
    ): TradeDataV3 {
        val bestTrade = quoter.bestTradeExactOut(rpcSource, chain, tokenIn, tokenOut, amountOut)
        return TradeDataV3(
            bestTrade,
            tradeOptions,
            priceImpactManager.getPriceImpact(rpcSource, chain, bestTrade)
        )
    }

    fun transactionData(
        receiveAddress: Address,
        chain: Chain,
        tradeData: TradeDataV3
    ) = swapRouter.transactionData(receiveAddress, chain, tradeData)

    companion object {
        fun getInstance(dexType: DexType): UniswapV3Kit {
            val tokenFactory = TokenFactory()
            val quoter = QuoterV2(tokenFactory, dexType)
            val swapRouter = SwapRouter(dexType)
            val poolManager = PoolManager(dexType)
            val priceImpactManager = PriceImpactManager(poolManager)

            return UniswapV3Kit(quoter, swapRouter, tokenFactory, priceImpactManager)
        }

        fun addDecorators(ethereumKit: EthereumKit) {
            ethereumKit.addMethodDecorator(UniswapV3MethodDecorator(UniswapV3ContractMethodFactories))
            ethereumKit.addTransactionDecorator(UniswapV3TransactionDecorator(TokenFactory.getWethAddress(ethereumKit.chain)))
        }

    }

}