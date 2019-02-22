package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.light.ByteUtils.xor
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.light.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.crypto.EciesCoder
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthAckMessage
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthMessage
import io.horizontalsystems.ethereumkit.light.toBytes
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.math.ec.ECPoint
import java.security.SecureRandom

class EncryptionHandshake(private val myKey: ECKey, private val remotePublicKeyPoint: ECPoint) {
    private val random = SecureRandom()
    private var initiatorNonce: ByteArray = ByteArray(32)
    private var ephemeralKey = CryptoUtils.randomECKey()

    var authMessagePacket: ByteArray = ByteArray(0)
    var authAckMessagePacket: ByteArray = ByteArray(0)

    companion object {
        const val NONCE_SIZE = 32
        const val MAC_SIZE = 256
        const val SECRET_SIZE = 32
    }

    init {
        random.nextBytes(initiatorNonce)
    }

    fun createAuthMessage() {
        val sharedSecret = CryptoUtils.keyAgreement(myKey.privateKey, remotePublicKeyPoint)

        val toBeSigned = xor(sharedSecret.toBytes(NONCE_SIZE), initiatorNonce)
        val signature = CryptoUtils.sign(ephemeralKey.privateKey, toBeSigned)

        val message = AuthMessage(signature, myKey.publicKeyPoint, initiatorNonce)

        authMessagePacket = encrypt(message)
    }

    fun handleAuthAckMessage(eciesEncryptedMessage: ECIESEncryptedMessage): Secrets {

        authAckMessagePacket = eciesEncryptedMessage.encoded()

        val decrypted = EciesCoder.decrypt(myKey.privateKey, eciesEncryptedMessage)
        val authAckMessage = AuthAckMessage.decode(decrypted)

        return agreeSecret(authAckMessage)
    }

    private fun agreeSecret(authAckMessage: AuthAckMessage): Secrets {
        val secretScalar = CryptoUtils.keyAgreement(ephemeralKey.privateKey, authAckMessage.publicKeyPoint)
        val agreedSecret = secretScalar.toBytes(SECRET_SIZE)
        val sharedSecret = CryptoUtils.sha3(agreedSecret, CryptoUtils.sha3(authAckMessage.nonce, initiatorNonce))
        val aesSecret = CryptoUtils.sha3(agreedSecret, sharedSecret)

        val secretsAes = aesSecret
        val secretsMac = CryptoUtils.sha3(agreedSecret, aesSecret)
        val secretsToken = CryptoUtils.sha3(sharedSecret)

        val secretsEgressMac = KeccakDigest(MAC_SIZE)
        secretsEgressMac.update(xor(secretsMac, authAckMessage.nonce), 0, secretsMac.size)
        val buf = ByteArray(32)
        KeccakDigest(secretsEgressMac).doFinal(buf, 0)
        secretsEgressMac.update(authMessagePacket, 0, authMessagePacket.size)
        KeccakDigest(secretsEgressMac).doFinal(buf, 0)

        val secretsIngressMac = KeccakDigest(MAC_SIZE)
        secretsIngressMac.update(xor(secretsMac, initiatorNonce), 0, secretsMac.size)
        KeccakDigest(secretsIngressMac).doFinal(buf, 0)
        secretsIngressMac.update(authAckMessagePacket, 0, authAckMessagePacket.size)
        KeccakDigest(secretsIngressMac).doFinal(buf, 0)

        return Secrets(secretsAes, secretsMac, secretsToken, secretsEgressMac, secretsIngressMac)
    }

    private fun encrypt(message: AuthMessage): ByteArray {
        val encodedMessage = message.encoded()
        val padded = eip8pad(encodedMessage)
        val encrypted = EciesCoder.encrypt(remotePublicKeyPoint, padded)
        return encrypted.encoded()
    }

    private fun eip8pad(msg: ByteArray): ByteArray {
        val randomBytes = ByteArray(random.nextInt(200) + 100)
        random.nextBytes(randomBytes)
        return msg + randomBytes
    }

}