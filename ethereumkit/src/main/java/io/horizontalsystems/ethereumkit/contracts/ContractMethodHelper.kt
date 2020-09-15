package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger
import kotlin.math.max

object ContractMethodHelper {
    fun getMethodId(methodSignature: String): ByteArray {
        return CryptoUtils.sha3(methodSignature.toByteArray()).copyOfRange(0, 4)
    }

    fun encodedABI(methodId: ByteArray, arguments: List<Any>): ByteArray {
        var data = methodId
        var arraysData = byteArrayOf()
        arguments.forEach { argument ->
            when (argument) {
                is BigInteger -> {
                    data += pad(argument.toByteArray())
                }
                is Address -> {
                    data += pad(argument.raw)
                }
                is List<*> -> {
                    val addresses = argument.filterIsInstance<Address>()

                    data += pad(BigInteger.valueOf(arguments.size * 32L + arraysData.size).toByteArray())
                    arraysData += encode(addresses)
                }
            }
        }
        return data + arraysData
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

    private fun encode(array: List<Address>): ByteArray {
        var data = pad(BigInteger.valueOf(array.size.toLong()).toByteArray())
        for (address in array) {
            data += pad(address.raw)
        }
        return data
    }
}