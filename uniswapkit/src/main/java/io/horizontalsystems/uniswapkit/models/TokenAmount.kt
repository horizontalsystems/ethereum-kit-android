package io.horizontalsystems.uniswapkit.models

import java.math.BigInteger

data class TokenAmount(val token: Token, val amount: BigInteger) {
    override fun toString(): String {
        return "{$token: ${amount.toBigDecimal().movePointLeft(token.decimals)}}"
    }
}
