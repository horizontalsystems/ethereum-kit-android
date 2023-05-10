package io.horizontalsystems.uniswapkit.v3

import io.horizontalsystems.uniswapkit.models.*
import io.horizontalsystems.uniswapkit.v3.quoter.BestTrade
import java.math.BigDecimal
import java.math.BigInteger

data class TradeDataV3(
    val trade: BestTrade,
    val options: TradeOptions,
    val priceImpact: BigDecimal?
) {
    val tradeType by trade::tradeType
    val swapPath by trade::swapPath
    val amountIn by trade::amountIn
    val amountOut by trade::amountOut
    val tokenIn by trade::tokenIn
    val tokenOut by trade::tokenOut
    val singleSwap by trade::singleSwap
    val singleSwapFee by trade::singleSwapFee

    val amountInMaximum: BigInteger
        get() = ((Fraction(BigInteger.ONE) + options.slippageFraction) * Fraction(trade.amountIn)).quotient
    val amountOutMinimum: BigInteger
        get() = ((Fraction(BigInteger.ONE) + options.slippageFraction).invert() * Fraction(trade.amountOut)).quotient

    val tokenAmountIn: TokenAmount
        get() = TokenAmount(tokenIn, amountIn)
    val tokenAmountOut: TokenAmount
        get() = TokenAmount(tokenOut, amountOut)

    val tokenAmountInMaximum: TokenAmount
        get() = TokenAmount(tokenIn, amountInMaximum)
    val tokenAmountOutMinimum: TokenAmount
        get() = TokenAmount(tokenOut, amountOutMinimum)

    val executionPrice = Price(baseTokenAmount = tokenAmountIn, quoteTokenAmount = tokenAmountOut).decimalValue?.stripTrailingZeros()

}
