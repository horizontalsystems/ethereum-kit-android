package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigDecimal
import java.math.BigInteger

class TradeOptions(
        allowedSlippagePercent: BigDecimal = BigDecimal("0.5"),
        var ttl: Long = 20 * 60,
        var recipient: Address? = null,
        var feeOnTransfer: Boolean = false
) {
    val allowedSlippagePercent: BigDecimal

    init {
        val strippedSlippage = allowedSlippagePercent.stripTrailingZeros()
        this.allowedSlippagePercent = strippedSlippage.setScale(strippedSlippage.scale() + 2)
    }

    val slippageFraction: Fraction
        get() = try {
            Fraction(allowedSlippagePercent / BigDecimal.valueOf(100))
        } catch (error: Exception) {
            Fraction(BigInteger.valueOf(5), BigInteger.valueOf(1000))
        }
}
