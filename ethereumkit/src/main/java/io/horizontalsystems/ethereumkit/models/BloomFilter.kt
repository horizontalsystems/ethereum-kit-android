package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.core.toInt
import kotlin.math.floor

class BloomFilter(val filter: String) {

    fun mayContainContractAddress(address: Address): Boolean {
        return mayContain(address.raw)
    }

    fun mayContainUserAddress(address: Address): Boolean {
        return mayContain(ByteArray(12) + address.raw)
    }

    private fun mayContain(element: ByteArray): Boolean {
        val hash = CryptoUtils.sha3(element)

        for (i in 0..2) {
            val startIndex = i * 2
            val bitPosition = ((byteArrayOf(hash[startIndex]).toInt() shl 8) + byteArrayOf(hash[startIndex + 1]).toInt()) and 2047
            val characterIndex = filter.length - 1 - floor(bitPosition / 4.0).toInt()
            val code = codePointToInt(filter[characterIndex].toInt()) ?: return false
            val offset = 1 shl (bitPosition % 4)

            if ((code and offset) != offset) {
                return false
            }
        }

        return true
    }

    private fun codePointToInt(codePoint: Int): Int? {
        if (codePoint in 48..57) {
            /* ['0'..'9'] -> [0..9] */
            return codePoint - 48;
        }

        if (codePoint in 65..70) {
            /* ['A'..'F'] -> [10..15] */
            return codePoint - 55;
        }

        if (codePoint in 97..102) {
            /* ['a'..'f'] -> [10..15] */
            return codePoint - 87;
        }

        return null
    }
}
