package io.horizontalsystems.ethereumkit.network

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger
import kotlin.math.max

object ERC20 {
    private fun buildMethodId(methodSignature: String): ByteArray {
        val hash = CryptoUtils.sha3(methodSignature.toByteArray())

        return hash.copyOfRange(0, 4)
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

    fun encodeFunctionBalanceOf(address: Address): ByteArray {
        val methodId = buildMethodId("balanceOf(address)")
        return methodId + pad(address.raw)
    }

    fun encodeFunctionTransfer(toAddress: Address, value: BigInteger): ByteArray {
        val methodId = buildMethodId("transfer(address,uint256)")
        return methodId + pad(toAddress.raw) + pad(value.toByteArray())
    }

    val transferEventTopic = CryptoUtils.sha3("Transfer(address,address,uint256)".toByteArray())

}
