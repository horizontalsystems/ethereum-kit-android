package io.horizontalsystems.uniswapkit.models

import java.math.BigDecimal
import java.math.BigInteger

class Price {
    private val baseToken: Token
    private val quoteToken: Token
    val value: Fraction
    private val scalar: Fraction

    constructor(baseToken: Token, quoteToken: Token, value: Fraction) {
        this.baseToken = baseToken
        this.quoteToken = quoteToken
        this.value = value

        scalar = Fraction(BigInteger.TEN.pow(baseToken.decimals), BigInteger.TEN.pow(quoteToken.decimals))
    }

    constructor(baseTokenAmount: TokenAmount, quoteTokenAmount: TokenAmount)
            : this(baseTokenAmount.token, quoteTokenAmount.token, Fraction(quoteTokenAmount.rawAmount, baseTokenAmount.rawAmount))

    val adjusted: Fraction
        get() = value * scalar

    val decimalValue: BigDecimal?
        get() = adjusted.toBigDecimal(quoteToken.decimals)

    operator fun times(other: Price): Price {
        return Price(this.baseToken, other.quoteToken, this.value * other.value)
    }

    override fun toString(): String {
        return "Price { baseToken: $baseToken, quoteToke: $quoteToken, value: ${decimalValue?.toPlainString()}"
    }

}
