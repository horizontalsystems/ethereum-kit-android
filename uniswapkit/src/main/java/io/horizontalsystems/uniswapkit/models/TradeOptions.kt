package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigDecimal
import java.math.BigInteger

class TradeOptions(
        var allowedSlippagePercent: BigDecimal = defaultAllowedSlippage,
        var ttl: Long = defaultTtl,
        var recipient: Address? = null,
        var feeOnTransfer: Boolean = false
) {

    val slippageFraction: Fraction
        get() = try {
            val strippedSlippage = allowedSlippagePercent.stripTrailingZeros()
            val scaledSlippage = strippedSlippage.setScale(strippedSlippage.scale() + 2)
            Fraction(scaledSlippage / BigDecimal.valueOf(100))
        } catch (error: Exception) {
            Fraction(BigInteger.valueOf(5), BigInteger.valueOf(1000))
        }

    companion object {
        val defaultAllowedSlippage = BigDecimal("0.5")
        val defaultTtl: Long = 20 * 60
    }
}
