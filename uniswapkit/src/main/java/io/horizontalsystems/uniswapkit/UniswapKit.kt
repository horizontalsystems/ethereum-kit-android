package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.contract.SwapContractMethodFactories
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import java.math.BigDecimal
import java.util.logging.Logger

class UniswapKit(
    private val tradeManager: TradeManager,
    private val pairSelector: PairSelector,
    private val tokenFactory: TokenFactory
) {
    private val logger = Logger.getLogger(this.javaClass.simpleName)

    fun routerAddress(chain: Chain): Address
         = TradeManager.getRouterAddress(chain)

    fun etherToken(chain: Chain): Token {
        return tokenFactory.etherToken(chain)
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return tokenFactory.token(contractAddress, decimals)
    }

    fun swapData(rpcSource: RpcSource, chain: Chain, tokenIn: Token, tokenOut: Token): Single<SwapData> {
        val tokenPairs = pairSelector.tokenPairs(chain, tokenIn, tokenOut)
        val singles = tokenPairs.map { (tokenA, tokenB) ->
            tradeManager.pair(rpcSource, chain, tokenA, tokenB)
        }

        return Single.zip(singles) { array ->
            val pairs = array.map { it as Pair }
            SwapData(pairs, tokenIn, tokenOut)
        }
    }

    fun bestTradeExactIn(swapData: SwapData, amountIn: BigDecimal, options: TradeOptions = TradeOptions()): TradeData {
        val tokenAmountIn = TokenAmount(swapData.tokenIn, amountIn)
        val sortedTrades = TradeManager.tradeExactIn(
            swapData.pairs,
            tokenAmountIn,
            swapData.tokenOut
        ).sorted()

        logger.info("bestTradeExactIn trades (${sortedTrades.size}):")
        sortedTrades.forEachIndexed { index, trade ->
            logger.info("$index: {in: ${trade.tokenAmountIn}, out: ${trade.tokenAmountOut}, impact: ${trade.priceImpact.toBigDecimal(2)}, pathSize: ${trade.route.path.size}")
        }

        val trade = sortedTrades.firstOrNull() ?: throw TradeError.TradeNotFound()
        logger.info("bestTradeExactIn path: ${trade.route.path.joinToString(" > ")}")

        return TradeData(trade, options)
    }

    fun bestTradeExactOut(swapData: SwapData, amountOut: BigDecimal, options: TradeOptions = TradeOptions()): TradeData {
        val tokenAmountOut = TokenAmount(swapData.tokenOut, amountOut)
        val sortedTrades = TradeManager.tradeExactOut(
            swapData.pairs,
            swapData.tokenIn,
            tokenAmountOut
        ).sorted()

        logger.info("bestTradeExactOut trades  (${sortedTrades.size}):")
        sortedTrades.forEachIndexed { index, trade ->
            logger.info("$index: {in: ${trade.tokenAmountIn}, out: ${trade.tokenAmountOut}, impact: ${trade.priceImpact}, pathSize: ${trade.route.path.size}")
        }

        val trade = sortedTrades.firstOrNull() ?: throw TradeError.TradeNotFound()
        logger.info("bestTradeExactOut path: ${trade.route.path.joinToString(" > ")}")

        return TradeData(trade, options)
    }

    fun transactionData(receiveAddress: Address, chain: Chain, tradeData: TradeData): TransactionData {
        return tradeManager.transactionData(receiveAddress, chain, tradeData)
    }

    companion object {
        fun getInstance(): UniswapKit {
            val tradeManager = TradeManager()
            val tokenFactory = TokenFactory()
            val pairSelector = PairSelector(tokenFactory)

            return UniswapKit(tradeManager, pairSelector, tokenFactory)
        }

        fun addDecorators(ethereumKit: EthereumKit) {
            ethereumKit.addMethodDecorator(SwapMethodDecorator(SwapContractMethodFactories))
            ethereumKit.addTransactionDecorator(SwapTransactionDecorator())
        }

    }

}

sealed class TradeError : Throwable() {
    class TradeNotFound : TradeError()
}

sealed class TokenAmountError : Throwable() {
    class NegativeAmount : TokenAmountError()
}

sealed class PairError : Throwable() {
    class NotInvolvedToken : PairError()
    class InsufficientReserves : PairError()
    class InsufficientReserveOut : PairError()
}

sealed class RouteError : Throwable() {
    class EmptyPairs : RouteError()
    class InvalidPair(val index: Int) : RouteError()
}
