package io.horizontalsystems.ethereumkit.light

import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun BigInteger.toBytes(numBytes: Int): ByteArray {
    val bytes = ByteArray(numBytes)
    val biBytes = this.toByteArray()
    val start = if (biBytes.size == numBytes + 1) 1 else 0
    val length = Math.min(biBytes.size, numBytes)
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