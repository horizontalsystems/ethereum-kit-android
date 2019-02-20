package io.horizontalsystems.ethereumkit.light

import kotlin.experimental.xor

object ByteUtils {

    fun merge(vararg arrays: ByteArray): ByteArray {
        var count = 0
        for (array in arrays) {
            count += array.size
        }

        val mergedArray = ByteArray(count)
        var start = 0
        for (array in arrays) {
            System.arraycopy(array, 0, mergedArray, start, array.size)
            start += array.size
        }
        return mergedArray
    }

    fun xor(b1: ByteArray, b2: ByteArray): ByteArray {
        val out = ByteArray(b1.size)
        for (i in b1.indices) {
            out[i] = (b1[i] xor (b2[i % b2.size]))
        }
        return out
    }
}