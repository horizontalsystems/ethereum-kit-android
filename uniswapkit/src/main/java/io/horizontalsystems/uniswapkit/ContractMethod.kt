package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import java.math.BigInteger
import kotlin.math.max

class ContractMethod(
        private val name: String,
        private val arguments: List<Argument> = listOf()
) {

    fun encodedABI(): ByteArray {
        var data = methodId()
        var arraysData = byteArrayOf()
        arguments.forEach { argument ->
            when (argument) {
                is Argument.Uint256 -> {
                    data += pad(argument.value.toByteArray())
                }
                is Argument.Address -> {
                    data += pad(argument.value)
                }
                is Argument.Addresses -> {
                    data += pad(BigInteger.valueOf(arguments.size * 32L + arraysData.size).toByteArray())
                    arraysData += encode(argument.values)
                }
            }
        }
        return data + arraysData
    }

    private fun methodId(): ByteArray {
        val methodSignature = name + "(" + arguments.joinToString(",") + ")"
        val hash = CryptoUtils.sha3(methodSignature.toByteArray())
        return hash.copyOfRange(0, 4)
    }

    private fun pad(data: ByteArray): ByteArray {
        val prePadding = ByteArray(max(0, 32 - data.size))
        return prePadding + data
    }

    private fun encode(array: List<ByteArray>): ByteArray {
        var data = pad(BigInteger.valueOf(array.size.toLong()).toByteArray())
        for (address in array) {
            data += pad(address)
        }
        return data
    }

    sealed class Argument {
        class Uint256(val value: BigInteger) : Argument() {
            override fun toString(): String {
                return "uint256"
            }
        }

        class Address(val value: ByteArray) : Argument() {
            override fun toString(): String {
                return "address"
            }
        }

        class Addresses(val values: List<ByteArray>) : Argument() {
            override fun toString(): String {
                return "address[]"
            }
        }
    }

}
