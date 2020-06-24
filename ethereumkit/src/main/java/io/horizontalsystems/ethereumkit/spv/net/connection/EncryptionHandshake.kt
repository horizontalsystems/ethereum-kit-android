package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.core.xor
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.net.connection.messages.AuthAckMessage
import io.horizontalsystems.ethereumkit.spv.net.connection.messages.AuthMessage
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.math.ec.ECPoint

class EncryptionHandshake(private val myKey: ECKey, private val remotePublicKeyPoint: ECPoint, private val cryptoUtils: CryptoUtils, private val randomHelper: RandomHelper) {

    open class HandshakeError : Exception() {
        class InvalidAuthAckPayload : HandshakeError()
    }

    companion object {
        const val MAC_SIZE = 256
    }

    private var initiatorNonce: ByteArray = randomHelper.randomBytes(32)
    private var ephemeralKey = randomHelper.randomECKey()
    private var authMessagePacket: ByteArray = ByteArray(0)

    fun createAuthMessage(): ByteArray {
        val sharedSecret = cryptoUtils.ecdhAgree(myKey, remotePublicKeyPoint)

        val toBeSigned = sharedSecret.xor(initiatorNonce)
        val signature = cryptoUtils.ellipticSign(toBeSigned, ephemeralKey.privateKey)

        val message = AuthMessage(signature, myKey.publicKeyPoint, initiatorNonce)

        authMessagePacket = encrypt(message)

        return authMessagePacket
    }

    fun handleAuthAckMessage(eciesEncryptedMessage: ECIESEncryptedMessage): Secrets {

        val decrypted = cryptoUtils.eciesDecrypt(myKey.privateKey, eciesEncryptedMessage)

        val authAckMessage = try {
            AuthAckMessage(decrypted)
        } catch (ex: Exception) {
            throw HandshakeError.InvalidAuthAckPayload()
        }

        return agreeSecret(authAckMessage, eciesEncryptedMessage.encoded())
    }

    private fun agreeSecret(authAckMessage: AuthAckMessage, authAckMessagePacket: ByteArray): Secrets {
        val agreedSecret = cryptoUtils.ecdhAgree(ephemeralKey, authAckMessage.ephemPublicKeyPoint)
        val sharedSecret = cryptoUtils.sha3(agreedSecret + cryptoUtils.sha3(authAckMessage.nonce + initiatorNonce))
        val secretsAes = cryptoUtils.sha3(agreedSecret + sharedSecret)

        val secretsMac = cryptoUtils.sha3(agreedSecret + secretsAes)
        val secretsToken = cryptoUtils.sha3(sharedSecret)

        val secretsEgressMac = KeccakDigest(MAC_SIZE)
        secretsEgressMac.update(secretsMac.xor(authAckMessage.nonce), 0, secretsMac.size)
        secretsEgressMac.update(authMessagePacket, 0, authMessagePacket.size)

        val secretsIngressMac = KeccakDigest(MAC_SIZE)
        secretsIngressMac.update(secretsMac.xor(initiatorNonce), 0, secretsMac.size)
        secretsIngressMac.update(authAckMessagePacket, 0, authAckMessagePacket.size)

        return Secrets(secretsAes, secretsMac, secretsToken, secretsEgressMac, secretsIngressMac)
    }

    private fun encrypt(message: AuthMessage): ByteArray {
        val encodedMessage = message.encoded() + eip8padding()
        val encrypted = cryptoUtils.eciesEncrypt(remotePublicKeyPoint, encodedMessage)
        return encrypted.encoded()
    }

    private fun eip8padding(): ByteArray {
        return randomHelper.randomBytes(200..300)
    }
}