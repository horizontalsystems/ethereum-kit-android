package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toInt
import java.math.BigInteger
import kotlin.math.max
import kotlin.reflect.KClass

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

    fun decodeABI(inputArguments: ByteArray, argumentTypes: List<KClass<out Any>>): List<Any> {
        var position = 0
        val parsedArguments = mutableListOf<Any>()
        argumentTypes.forEach { type ->
            when (type) {
                BigInteger::class -> {
                    parsedArguments.add(inputArguments.copyOfRange(position, position + 32).toBigInteger())
                }
                Address::class -> {
                    parsedArguments.add(parseAddress(inputArguments.copyOfRange(position, position + 32)))
                }
                List::class -> {
                    val arrayPosition = inputArguments.copyOfRange(position, position + 32).toInt()
                    val array = parseAddressArray(arrayPosition, inputArguments)
                    parsedArguments.add(array)
                }
            }
            position += 32
        }

        return parsedArguments
    }

    private fun parseAddress(address: ByteArray): Address {
        return Address(address.copyOfRange(address.size - 20, address.size))
    }

    private fun parseAddressArray(positionStart: Int, inputArguments: ByteArray): List<Address> {
        val sizePositionEnd = positionStart + 32
        val arraySize = inputArguments.copyOfRange(positionStart, sizePositionEnd).toInt()
        val addressArray = mutableListOf<Address>()
        for (address in inputArguments.copyOfRange(sizePositionEnd, sizePositionEnd + arraySize * 32).toList().chunked(32)) {
            addressArray.add(parseAddress(address.toByteArray()))
        }
        return addressArray
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