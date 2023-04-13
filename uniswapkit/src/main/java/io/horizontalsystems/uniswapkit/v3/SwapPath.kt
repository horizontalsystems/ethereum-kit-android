package io.horizontalsystems.uniswapkit.v3

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger
import kotlin.math.max

data class SwapPath(val items: List<SwapPathItem>) {

    fun abiEncodePacked(): ByteArray {
        var res = items.first().token1.raw

        items.forEach {
            res += encodeUnit24(it.fee.value) + it.token2.raw
        }

        return res
    }

    private fun encodeUnit24(v: BigInteger): ByteArray {
        val data = ContractMethodHelper.unsignedBigIntergerToByteArray(v)
        val prePadding = ByteArray(max(0, 3 - data.size))
        return prePadding + data
    }
}

data class SwapPathItem(val token1: Address, val token2: Address, val fee: FeeAmount)
