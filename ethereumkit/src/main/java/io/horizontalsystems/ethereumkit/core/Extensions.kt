package io.horizontalsystems.ethereumkit.core

import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
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
    return this.getByteArray()
}

@Throws(NumberFormatException::class)
fun String.hexStringToByteArrayOrNull(): ByteArray? {
    return try {
        this.getByteArray()
    } catch (error: Throwable) {
        null
    }
}

private fun String.getByteArray(): ByteArray {
    var hexWithoutPrefix = this.stripHexPrefix()
    if (hexWithoutPrefix.length % 2 == 1) {
        hexWithoutPrefix = "0$hexWithoutPrefix"
    }
    return hexWithoutPrefix.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
}

fun String.stripHexPrefix(): String {
    return if (this.startsWith("0x", true)) {
        this.substring(2)
    } else {
        this
    }
}

fun Long.toHexString(): String {
    return "0x${this.toString(16)}"
}

fun Int.toHexString(): String {
    return "0x${this.toString(16)}"
}

fun String.hexStringToLongOrNull(): Long? {
    return this.stripHexPrefix().toLongOrNull(16)
}

fun String.hexStringToIntOrNull(): Int? {
    return this.stripHexPrefix().toIntOrNull(16)
}

fun BigInteger.toHexString(): String {
    return "0x${this.toString(16)}"
}

fun String.hexStringToBigIntegerOrNull(): BigInteger? {
    return this.stripHexPrefix().toBigIntegerOrNull(16)
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

object MustRetry : Exception()

data class RetryOptions<T : Any>(
        val maxRetryCount: Int = 3,
        val delayTime: Long = 5, //seconds
        val delayTimeIncreaseFactor: Int = 3,
        val mustRetry: (T) -> Boolean
)

fun <T : Any> Single<T>.retryWith(options: RetryOptions<T>): Single<T> {
    val retryCount = AtomicInteger(1)

    return delay(options.delayTime, TimeUnit.SECONDS)
            .map {
                if (options.mustRetry(it) && retryCount.getAndIncrement() < options.maxRetryCount)
                    throw MustRetry
                it
            }
            .retryWhen { errors ->
                val delayTime = AtomicLong(options.delayTime)

                errors.takeWhile { it == MustRetry }
                        .flatMap {
                            delayTime.set(delayTime.get() * options.delayTimeIncreaseFactor)
                            Flowable.timer(delayTime.get(), TimeUnit.SECONDS)
                        }
            }
}
