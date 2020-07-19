package io.horizontalsystems.uniswapkit.models

import java.math.BigInteger

class Trade(
        val type: TradeType,
        val route: Route,
        val tokenAmountIn: TokenAmount,
        val tokenAmountOut: TokenAmount
) {
    val executionPrice = Price(baseTokenAmount = tokenAmountIn, quoteTokenAmount = tokenAmountOut)
    val priceImpact = computePriceImpact(route.midPrice, tokenAmountIn, tokenAmountOut)

    companion object {
        private fun computePriceImpact(midPrice: Price, tokenAmountIn: TokenAmount, tokenAmountOut: TokenAmount): Fraction {
            val exactQuote = midPrice.value * Fraction(tokenAmountIn.rawAmount) * Fraction(BigInteger.valueOf(997), BigInteger.valueOf(1000))
            return (exactQuote - Fraction(tokenAmountOut.rawAmount)) / exactQuote * Fraction(BigInteger.valueOf(100))
        }
    }
}
