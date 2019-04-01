package io.horizontalsystems.ethereumkit.network

import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import java.lang.Math.max

object ERC20 {
    private fun buildMethodId(methodSignature: String): ByteArray {
        val hash = CryptoUtils.sha3(methodSignature.toByteArray())

        return hash.copyOfRange(0, 4)
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

    fun encodeFunctionBalanceOf(address: ByteArray): ByteArray {
        val methodId = buildMethodId("balanceOf(address)")
        return methodId + pad(address)
    }
}
