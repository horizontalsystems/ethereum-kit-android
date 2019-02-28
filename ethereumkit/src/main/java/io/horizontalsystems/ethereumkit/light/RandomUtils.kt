package io.horizontalsystems.ethereumkit.light

import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import java.security.SecureRandom

class RandomUtils {
    fun randomKey(): ECKey {
        return CryptoUtils.randomECKey()
    }

    fun randomBytes(length: Int): ByteArray {
        val randomBytes = ByteArray(length)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes
    }

    fun randomBytes(lengthRange: IntRange): ByteArray {
        val length = SecureRandom().nextInt(lengthRange.last) + lengthRange.first
        return randomBytes(length)
    }
}