package io.horizontalsystems.uniswapkit.v3

import io.horizontalsystems.uniswapkit.models.DexType
import java.math.BigInteger

sealed class FeeAmount(val value: BigInteger) {
    object LOWEST : FeeAmount(100.toBigInteger())
    object LOW : FeeAmount(500.toBigInteger())
    object MEDIUM_PANCAKESWAP : FeeAmount(2500.toBigInteger())
    object MEDIUM_UNISWAP : FeeAmount(3000.toBigInteger())
    object HIGH : FeeAmount(10000.toBigInteger())

    companion object {
        fun sorted(dexType: DexType) = listOf(
            LOWEST,
            LOW,
            when (dexType) {
                DexType.Uniswap -> MEDIUM_UNISWAP
                DexType.PancakeSwap -> MEDIUM_PANCAKESWAP
            },
            HIGH
        )
    }
}
