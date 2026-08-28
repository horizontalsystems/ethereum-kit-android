package io.horizontalsystems.ethereumkit.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import java.math.BigInteger
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

/**
 * Runs [block], retrying up to [maxRetries] times (with a linearly increasing delay of 1s, 2s, 3s...)
 * whenever it throws an exception of exactly class [errorForRetry]. Any other error is rethrown immediately.
 */
suspend fun <T> retryWhenError(errorForRetry: KClass<*>, maxRetries: Int = 3, block: suspend () -> T): T {
    var retryCounter = 0L
    while (true) {
        try {
            return block()
        } catch (error: Throwable) {
            if (errorForRetry == error::class && retryCounter++ < maxRetries) {
                delay(retryCounter * 1000)
            } else {
                throw error
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

/**
 * Waits [RetryOptions.delayTime] seconds, runs [block], and re-runs it (with the delay multiplied by
 * [RetryOptions.delayTimeIncreaseFactor] each time) while [RetryOptions.mustRetry] returns true for the result,
 * at most [RetryOptions.maxRetryCount] times in total.
 */
suspend fun <T : Any> retryWith(options: RetryOptions<T>, block: suspend () -> T): T {
    var delayTime = options.delayTime
    var retryCount = 1

    delay(delayTime * 1000)
    while (true) {
        val result = block()
        if (options.mustRetry(result) && retryCount++ < options.maxRetryCount) {
            delayTime *= options.delayTimeIncreaseFactor
            delay(delayTime * 1000)
        } else {
            return result
        }
    }
}

/**
 * Equivalent of an RxJava `PublishSubject.toFlowable(BackpressureStrategy.BUFFER)`: emissions are dropped when
 * there are no collectors, buffered without limit otherwise, and [MutableSharedFlow.tryEmit] never fails.
 */
fun <T> bufferedSharedFlow(): MutableSharedFlow<T> = MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
