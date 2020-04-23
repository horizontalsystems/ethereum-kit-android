package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.spv.rlp.RLPElement
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.experimental.xor
import kotlin.math.min

fun BigInteger.toBytes(numBytes: Int): ByteArray {
    val bytes = ByteArray(numBytes)
    val biBytes = this.toByteArray()
    val start = if (biBytes.size == numBytes + 1) 1 else 0
    val length = min(biBytes.size, numBytes)
    System.arraycopy(biBytes, start, bytes, numBytes - length, length)
    return bytes
}

fun Short.toBytes(): ByteArray {
    return ByteBuffer.allocate(2).putShort(this).array()
}

fun ByteArray.toShort(): Short {
    val bb = ByteBuffer.wrap(this)
    bb.order(ByteOrder.BIG_ENDIAN)

    return bb.short
}

fun ByteArray?.toInt(): Int {
    return if (this == null || this.isEmpty()) 0 else BigInteger(1, this).toInt()
}

fun ByteArray?.toLong(): Long {
    return if (this == null || this.isEmpty()) 0 else BigInteger(1, this).toLong()
}

fun ByteArray?.toBigInteger(): BigInteger {
    return if (this == null || this.isEmpty()) BigInteger.ZERO else BigInteger(1, this)
}

fun RLPElement?.toInt(): Int {
    val rlpData = this?.rlpData
    return if (this == null || rlpData == null || rlpData.isEmpty()) 0 else BigInteger(1, rlpData).toInt()
}

fun RLPElement?.toLong(): Long {
    val rlpData = this?.rlpData
    return if (this == null || rlpData == null || rlpData.isEmpty()) 0 else BigInteger(1, rlpData).toLong()
}

fun RLPElement?.toBigInteger(): BigInteger {
    val rlpData = this?.rlpData
    return if (this == null || rlpData == null || rlpData.isEmpty()) BigInteger.ZERO else BigInteger(1, rlpData)
}

fun RLPElement?.asString(): String {
    val rlpData = this?.rlpData
    return if (this == null || rlpData == null || rlpData.isEmpty()) "" else String(rlpData)
}

fun ByteArray.xor(other: ByteArray): ByteArray {
    val out = ByteArray(this.size)
    for (i in this.indices) {
        out[i] = (this[i] xor (other[i % other.size]))
    }
    return out
}

fun Int.toBytesNoLeadZeroes(): ByteArray {
    var value = this

    if (value == 0) return byteArrayOf()

    var length = 0

    var tmpVal = value
    while (tmpVal != 0) {
        tmpVal = tmpVal.ushr(8)
        ++length
    }

    val result = ByteArray(length)

    var index = result.size - 1
    while (value != 0) {

        result[index] = (value and 0xFF).toByte()
        value = value.ushr(8)
        index -= 1
    }

    return result
}