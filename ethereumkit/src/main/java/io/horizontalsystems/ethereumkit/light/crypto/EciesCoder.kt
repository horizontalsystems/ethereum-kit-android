package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.light.toBytes
import org.spongycastle.crypto.BufferedBlockCipher
import org.spongycastle.crypto.InvalidCipherTextException
import org.spongycastle.crypto.agreement.ECDHBasicAgreement
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.*
import org.spongycastle.math.ec.ECPoint
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom

object EciesCoder {

    private const val KEY_SIZE = 128
    private const val PREFIX_SIZE = 65 + KEY_SIZE / 8 + 32 // 256 bit EC public key, IV, 256 bit MAC

    fun encrypt(remotePublicKey: ECPoint, msg: ByteArray): ECIESEncryptedMessage {
        val size = msg.size + EciesCoder.PREFIX_SIZE
        val prefixBytes = size.toShort().toBytes()

        val eGen = ECKeyPairGenerator()
        val random = SecureRandom()
        val gParam = ECKeyGenerationParameters(CURVE, random)
        eGen.init(gParam)

        val iv = ByteArray(KEY_SIZE / 8)
        SecureRandom().nextBytes(iv)

        val ephemPair = eGen.generateKeyPair()
        val prv = (ephemPair.private as ECPrivateKeyParameters).d
        val pub = (ephemPair.public as ECPublicKeyParameters).q
        val iesEngine = makeIESEngine(true, remotePublicKey, prv, iv)

        val cipher = iesEngine.processBlock(msg, 0, msg.size, prefixBytes)

        return ECIESEncryptedMessage(prefixBytes,
                pub.getEncoded(false),
                iv,
                cipher.copyOfRange(0, cipher.size - 32),
                cipher.copyOfRange(cipher.size - 32, cipher.size))
    }

    @Throws(IOException::class, InvalidCipherTextException::class)
    fun decrypt(privKey: BigInteger, encryptedMessage: ECIESEncryptedMessage): ByteArray {

        val ephem = CURVE.curve.decodePoint(encryptedMessage.ephemeralPubKey)

        val iesEngine = makeIESEngine(false, ephem, privKey, encryptedMessage.initialVector)

        val cipherBody = encryptedMessage.cipher + encryptedMessage.checkSum

        return iesEngine.processBlock(cipherBody, 0, cipherBody.size, encryptedMessage.prefixBytes)
    }

    private fun makeIESEngine(isEncrypt: Boolean, pub: ECPoint, prv: BigInteger, IV: ByteArray): IESEngine {
        val aesFastEngine = AESEngine()

        val iesEngine = IESEngine(
                ECDHBasicAgreement(),
                ConcatKDFBytesGenerator(SHA256Digest()),
                HMac(SHA256Digest()),
                SHA256Digest(),
                BufferedBlockCipher(SICBlockCipher(aesFastEngine)))


        val d = byteArrayOf()
        val e = byteArrayOf()

        val p = IESWithCipherParameters(d, e, KEY_SIZE, KEY_SIZE)
        val parametersWithIV = ParametersWithIV(p, IV)

        iesEngine.init(isEncrypt, ECPrivateKeyParameters(prv, CURVE), ECPublicKeyParameters(pub, CURVE), parametersWithIV)
        return iesEngine
    }
}