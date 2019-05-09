package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.utils.EIP55

fun ByteArray?.toRawHexString(): String {
    return this?.joinToString(separator = "") {
        it.toInt().and(0xff).toString(16).padStart(2, '0')
    } ?: ""
}

fun ByteArray?.toHexString(): String {
    val rawHex = this?.toRawHexString() ?: return ""
    return "0x$rawHex"
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArray(): ByteArray {
    val hexWithoutPrefix = this.stripHexPrefix()
    return ByteArray(hexWithoutPrefix.length / 2) {
        hexWithoutPrefix.substring(it * 2, it * 2 + 2).toInt(16).toByte()
    }
}

fun String.stripHexPrefix(): String {
    return if (this.startsWith("0x", true)) {
        this.substring(2)
    } else {
        this
    }
}

fun ByteArray.toEIP55Address(): String {
    return EIP55.encode(this)
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
