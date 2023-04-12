package io.horizontalsystems.uniswapkit.v3

import java.math.BigInteger

sealed class FeeAmount(val value: BigInteger) {
    object LOWEST : FeeAmount(100.toBigInteger())
    object LOW : FeeAmount(500.toBigInteger())
    object MEDIUM : FeeAmount(3000.toBigInteger())
    object HIGH : FeeAmount(10000.toBigInteger())

    companion object {
        fun sorted() = listOf(LOWEST, LOW, MEDIUM, HIGH)
    }
}
