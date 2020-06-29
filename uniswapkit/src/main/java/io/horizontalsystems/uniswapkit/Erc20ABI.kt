package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import java.math.BigInteger
import kotlin.math.max

object Erc20ABI {

    fun approve(spenderAddress: ByteArray, amount: BigInteger): ByteArray {
        val methodId = CryptoUtils.sha3("approve(address,uint256)".toByteArray()).copyOfRange(0, 4)
        return methodId +
                pad(spenderAddress) +
                pad(amount.toByteArray())
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

}
