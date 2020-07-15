package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.uniswapkit.models.*
import io.reactivex.Single
import java.math.BigInteger
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

    fun bestTradeExactIn(swapData: SwapData, amountIn: BigInteger, options: TradeOptions = TradeOptions()): TradeData? {
        val tokenAmountIn = TokenAmount(swapData.tokenIn, amountIn)
        try {
            val trades = TradeManager.bestTradeExactIn(
                    swapData.pairs,
                    tokenAmountIn,
                    swapData.tokenOut
            )
            val trade = trades.firstOrNull() ?: return null
            logger.info("bestTradeExactIn path: ${trade.route.path.joinToString(" > ")}")

            return TradeData(trade, options)
        } catch (error: Throwable) {
            logger.warning("bestTradeExactIn error: ${error.message}")
            return null
        }
    }

    fun bestTradeExactOut(swapData: SwapData, amountOut: BigInteger, options: TradeOptions = TradeOptions()): TradeData? {
        val tokenAmountOut = TokenAmount(swapData.tokenOut, amountOut)
        try {
            val trades = TradeManager.bestTradeExactOut(
                    swapData.pairs,
                    swapData.tokenIn,
                    tokenAmountOut
            )
            val trade = trades.firstOrNull() ?: return null
            logger.info("bestTradeExactOut path: ${trade.route.path.joinToString(" > ")}")

            return TradeData(trade, options)
        } catch (error: Throwable) {
            logger.warning("bestTradeExactOut error: ${error.message}")
            return null
        }
    }

    fun swap(tradeData: TradeData): Single<String> {
        return tradeManager.swap(tradeData)
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

sealed class UniswapKitError : Throwable() {
    class InsufficientReserve : UniswapKitError()
}
