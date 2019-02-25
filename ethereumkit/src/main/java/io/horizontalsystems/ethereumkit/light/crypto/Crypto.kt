package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.light.toBytes
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.SecureRandom

class Crypto : ICrypto {

    companion object {
        const val SECRET_SIZE = 32
    }

    override fun randomKey(): ECKey {
        return CryptoUtils.randomECKey()
    }

    override fun randomBytes(length: Int): ByteArray {
        val randomBytes = ByteArray(length)
        SecureRandom().nextBytes(randomBytes)
        return randomBytes
    }

    override fun ecdhAgree(myKey: ECKey, remotePublicKeyPoint: ECPoint): ByteArray {
        return CryptoUtils.keyAgreement(myKey.privateKey, remotePublicKeyPoint).toBytes(SECRET_SIZE)
    }

    override fun ellipticSign(messageToSign: ByteArray, key: ECKey): ByteArray {
        return CryptoUtils.sign(key.privateKey, messageToSign)
    }

    override fun eciesDecrypt(privateKey: BigInteger, message: ECIESEncryptedMessage): ByteArray {
        return EciesCoder.decrypt(privateKey, message)
    }

    override fun eciesEncrypt(remotePublicKey: ECPoint, message: ByteArray): ECIESEncryptedMessage {
        return EciesCoder.encrypt(remotePublicKey, message)
    }

    override fun sha3(data: ByteArray): ByteArray {
        return CryptoUtils.sha3(data)
    }
}