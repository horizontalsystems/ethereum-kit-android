package io.horizontalsystems.ethereumkit.spv.helpers

import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.params.ECKeyGenerationParameters
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.params.ECPublicKeyParameters
import java.security.SecureRandom

object RandomHelper {
    fun randomECKey(): ECKey {
        val eGen = ECKeyPairGenerator()
        val random = SecureRandom()
        val gParam = ECKeyGenerationParameters(CURVE, random)

        eGen.init(gParam)

        val keyPair = eGen.generateKeyPair()
        val prv = (keyPair.private as ECPrivateKeyParameters).d
        val pub = (keyPair.public as ECPublicKeyParameters).q

        return ECKey(prv, pub)
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