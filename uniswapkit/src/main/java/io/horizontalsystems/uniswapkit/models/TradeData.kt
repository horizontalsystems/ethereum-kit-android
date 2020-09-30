package io.horizontalsystems.uniswapkit.models

import java.math.BigDecimal
import java.math.BigInteger

class TradeData(
        val trade: Trade,
        val options: TradeOptions
) {

    val tokenAmountInMax: TokenAmount
        get() {
            val amountInMax = ((Fraction(BigInteger.ONE) + options.slippageFraction) * Fraction(trade.tokenAmountIn.rawAmount)).quotient
            return TokenAmount(trade.tokenAmountIn.token, amountInMax)
        }

    val tokenAmountOutMin: TokenAmount
        get() {
            val amountOutMin = ((Fraction(BigInteger.ONE) + options.slippageFraction).invert() * Fraction(trade.tokenAmountOut.rawAmount)).quotient
            return TokenAmount(trade.tokenAmountOut.token, amountOutMin)
        }

    val type = trade.type

    val amountIn: BigDecimal? = trade.tokenAmountIn.decimalAmount?.stripTrailingZeros()

    val amountOut: BigDecimal? = trade.tokenAmountOut.decimalAmount?.stripTrailingZeros()

    val amountInMax: BigDecimal? = tokenAmountInMax.decimalAmount?.stripTrailingZeros()

    val amountOutMin: BigDecimal? = tokenAmountOutMin.decimalAmount?.stripTrailingZeros()

    val executionPrice: BigDecimal? = trade.executionPrice.decimalValue?.stripTrailingZeros()

    val midPrice: BigDecimal? = trade.route.midPrice.decimalValue?.stripTrailingZeros()

    val priceImpact: BigDecimal? = trade.priceImpact.toBigDecimal(2)

    val providerFee: BigDecimal?
        get() {
            val amount = (if (type == TradeType.ExactIn) amountIn else amountInMax) ?: return null

            return trade.liquidityProviderFee.toBigDecimal(trade.tokenAmountIn.token.decimals)?.let {
                it * amount
            }
        }

    val path: List<Token> = trade.route.path

}
