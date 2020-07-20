package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
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

    fun etherToken(): Token {
        return tokenFactory.etherToken()
    }

    fun token(contractAddress: ByteArray, decimals: Int): Token {
        return tokenFactory.token(contractAddress, decimals)
    }

    fun swapData(tokenIn: Token, tokenOut: Token): Single<SwapData> {
        val tokenPairs = pairSelector.tokenPairs(tokenIn, tokenOut)
        val singles = tokenPairs.map { (tokenA, tokenB) ->
            tradeManager.getPair(tokenA, tokenB)
        }

        return Single.zip(singles) { array ->
            val pairs = array.map { it as Pair }
            SwapData(pairs, tokenIn, tokenOut)
        }
    }

    fun bestTradeExactIn(swapData: SwapData, amountIn: BigDecimal, options: TradeOptions = TradeOptions()): TradeData {
        val tokenAmountIn = TokenAmount(swapData.tokenIn, amountIn)
        val trades = TradeManager.bestTradeExactIn(
                swapData.pairs,
                tokenAmountIn,
                swapData.tokenOut
        )
        val trade = trades.firstOrNull() ?: throw BestTradeError.TradeNotFound()

        logger.info("bestTradeExactIn trades.size: ${trades.size}")
        logger.info("bestTradeExactIn path: ${trade.route.path.joinToString(" > ")}")

        return TradeData(trade, options)
    }

    fun bestTradeExactOut(swapData: SwapData, amountOut: BigDecimal, options: TradeOptions = TradeOptions()): TradeData {
        val tokenAmountOut = TokenAmount(swapData.tokenOut, amountOut)
        val trades = TradeManager.bestTradeExactOut(
                swapData.pairs,
                swapData.tokenIn,
                tokenAmountOut
        )
        val trade = trades.firstOrNull() ?: throw BestTradeError.TradeNotFound()

        logger.info("bestTradeExactOut trades.size: ${trades.size}")
        logger.info("bestTradeExactOut path: ${trade.route.path.joinToString(" > ")}")

        return TradeData(trade, options)
    }

    fun swap(tradeData: TradeData, gasPrice: Long): Single<String> {
        return tradeManager.swap(tradeData, gasPrice)
    }

    companion object {
        fun getInstance(ethereumKit: EthereumKit, networkType: EthereumKit.NetworkType): UniswapKit {
            val tradeManager = TradeManager(ethereumKit)
            val tokenFactory = TokenFactory(networkType)
            val pairSelector = PairSelector(tokenFactory)
            return UniswapKit(tradeManager, pairSelector, tokenFactory)
        }
    }

}

sealed class BestTradeError : Throwable() {
    class TradeNotFound : BestTradeError()
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
