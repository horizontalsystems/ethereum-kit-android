package io.horizontalsystems.uniswapkit.models

import java.math.BigDecimal
import java.math.BigInteger

class TradeData(
        val trade: Trade,
        val options: TradeOptions
) {

    val tokenAmountInMax = TokenAmount(
            trade.tokenAmountIn.token,
            trade.tokenAmountIn.amount * ((100_00 + options.allowedSlippage * 100L).toBigDecimal().divide(BigDecimal.valueOf(100_00))).toBigInteger()
    )

    val tokenAmountOutMin = TokenAmount(
            trade.tokenAmountOut.token,
            trade.tokenAmountOut.amount * ((100_00 - options.allowedSlippage * 100L).toBigDecimal().divide(BigDecimal.valueOf(100_00))).toBigInteger()
    )

    val type = trade.type

    val amountIn: String
        get() {
            val token = trade.tokenAmountIn.token
            val amount = trade.tokenAmountIn.amount

            return if (amount == BigInteger.ZERO)
                "0"
            else
                amount.toBigDecimal().movePointLeft(token.decimals).stripTrailingZeros().toPlainString()
        }

    val amountOut: String
        get() {
            val token = trade.tokenAmountOut.token
            val amount = trade.tokenAmountOut.amount

            return if (amount == BigInteger.ZERO)
                "0"
            else
                amount.toBigDecimal().movePointLeft(token.decimals).stripTrailingZeros().toPlainString()
        }

    val amountInMax: String = tokenAmountInMax.amount.toString()

    val amountOutMin: String = tokenAmountOutMin.amount.toString()
}
