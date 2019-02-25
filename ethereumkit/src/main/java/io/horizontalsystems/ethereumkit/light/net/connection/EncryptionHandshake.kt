package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.light.ByteUtils.xor
import io.horizontalsystems.ethereumkit.light.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.crypto.ICrypto
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthAckMessage
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthMessage
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.math.ec.ECPoint
import java.security.SecureRandom

class EncryptionHandshake(private val myKey: ECKey, private val remotePublicKeyPoint: ECPoint, private val crypto: ICrypto) {
    private var initiatorNonce: ByteArray = crypto.randomBytes(32)
    private var ephemeralKey = crypto.randomKey()

    private var authMessagePacket: ByteArray = ByteArray(0)

    companion object {
        const val MAC_SIZE = 256
    }

    fun createAuthMessage(): ByteArray {
        val sharedSecret = crypto.ecdhAgree(myKey, remotePublicKeyPoint)

        val toBeSigned = xor(sharedSecret, initiatorNonce)
        val signature = crypto.ellipticSign(toBeSigned, ephemeralKey)

        val message = AuthMessage(signature, myKey.publicKeyPoint, initiatorNonce)

        authMessagePacket = encrypt(message)

        return authMessagePacket
    }

    fun handleAuthAckMessage(eciesEncryptedMessage: ECIESEncryptedMessage): Secrets {

        val decrypted = crypto.eciesDecrypt(myKey.privateKey, eciesEncryptedMessage)
        val authAckMessage = AuthAckMessage.decode(decrypted)

        return agreeSecret(authAckMessage, eciesEncryptedMessage.encoded())
    }

    private fun agreeSecret(authAckMessage: AuthAckMessage, authAckMessagePacket: ByteArray): Secrets {
        val agreedSecret = crypto.ecdhAgree(ephemeralKey, authAckMessage.publicKeyPoint)
        val sharedSecret = crypto.sha3(agreedSecret + crypto.sha3(authAckMessage.nonce + initiatorNonce))
        val secretsAes = crypto.sha3(agreedSecret + sharedSecret)

        val secretsMac = crypto.sha3(agreedSecret + secretsAes)
        val secretsToken = crypto.sha3(sharedSecret)

        val secretsEgressMac = KeccakDigest(MAC_SIZE)
        secretsEgressMac.update(xor(secretsMac, authAckMessage.nonce), 0, secretsMac.size)
        secretsEgressMac.update(authMessagePacket, 0, authMessagePacket.size)

        val secretsIngressMac = KeccakDigest(MAC_SIZE)
        secretsIngressMac.update(xor(secretsMac, initiatorNonce), 0, secretsMac.size)
        secretsIngressMac.update(authAckMessagePacket, 0, authAckMessagePacket.size)

        return Secrets(secretsAes, secretsMac, secretsToken, secretsEgressMac, secretsIngressMac)
    }

    private fun encrypt(message: AuthMessage): ByteArray {
        val encodedMessage = message.encoded() + eip8padding()
        val encrypted = crypto.eciesEncrypt(remotePublicKeyPoint, encodedMessage)
        return encrypted.encoded()
    }

    private fun eip8padding(): ByteArray {
        val junkLength = SecureRandom().nextInt(200) + 100
        return crypto.randomBytes(junkLength)
    }
}