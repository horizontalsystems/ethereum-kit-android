package io.horizontalsystems.ethereumkit.light

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
}