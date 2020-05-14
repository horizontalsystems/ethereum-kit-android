package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.utils.EIP55
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass


fun String.removeLeadingZeros(): String {
    return this.trimStart('0')
}

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

// Converts positive long values to a byte array without leading zero byte (for sign bit)
fun Long.toByteArray(): ByteArray {
    var array = this.toBigInteger().toByteArray()
    if (array[0].toInt() == 0) {
        val tmp = ByteArray(array.size - 1)
        System.arraycopy(array, 1, tmp, 0, tmp.size)
        array = tmp
    }
    return array
}

fun <T> Single<T>.retryWhenError(errorForRetry: KClass<*>, maxRetries: Int = 3): Single<T> {
    return retryWhen { errors ->
        var retryCounter = 0L
        errors.flatMap { error ->
            if (errorForRetry == error::class && retryCounter++ < maxRetries) {
                Flowable.timer(retryCounter, TimeUnit.SECONDS)
            } else {
                Flowable.error(error)
            }
        }
    }
}
