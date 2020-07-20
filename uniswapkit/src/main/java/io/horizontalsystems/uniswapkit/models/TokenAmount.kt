package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.uniswapkit.TokenAmountError
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue

class TokenAmount : Comparable<TokenAmount> {
    val token: Token
    val amount: Fraction

    constructor(token: Token, rawAmount: BigInteger) {
        check(rawAmount.signum() >= 0) {
            throw TokenAmountError.NegativeAmount()
        }
        this.token = token
        this.amount = Fraction(rawAmount, BigInteger.TEN.pow(token.decimals))
    }

    constructor(token: Token, decimal: BigDecimal) : this(token, getRawAmount(token, decimal))

    val rawAmount: BigInteger
        get() = amount.numerator

    val decimalAmount: BigDecimal?
        get() = amount.toBigDecimal(token.decimals)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        return if (other is TokenAmount) {
            this.compareTo(other) == 0
        } else false
    }

    override fun compareTo(other: TokenAmount): Int {
        check(this.token == other.token)

        return this.amount.compareTo(other.amount)
    }

    override fun toString(): String {
        return "{$token: ${decimalAmount?.stripTrailingZeros()}}"
    }

    companion object {
        private fun getRawAmount(token: Token, decimal: BigDecimal): BigInteger {
            val exponent = token.decimals - decimal.scale()

            return if (exponent >= 0) {
                decimal.unscaledValue() * BigInteger.TEN.pow(exponent)
            } else {
                decimal.unscaledValue() / BigInteger.TEN.pow(exponent.absoluteValue)
            }
        }
    }

}
