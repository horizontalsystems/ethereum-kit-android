package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils.CURVE
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom

object EciesCoder {

    const val KEY_SIZE = 128
    const val PREFIX_SIZE = 65 + KEY_SIZE / 8 + 32 // 256 bit EC public key, IV, 256 bit MAC

    fun encrypt(remotePublicKey: ECPoint, msg: ByteArray, macData: ByteArray): ByteArray {

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

        val cipher: ByteArray
        try {
            cipher = iesEngine.processBlock(msg, 0, msg.size, macData)
            val bos = ByteArrayOutputStream()
            bos.write(pub.getEncoded(false))
            bos.write(iv)
            bos.write(cipher)
            return bos.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ByteArray(0)
    }

    @Throws(IOException::class, InvalidCipherTextException::class)
    fun decrypt(privKey: BigInteger, cipher: ByteArray, macData: ByteArray?): ByteArray {

        val plaintext: ByteArray

        val inputStream = ByteArrayInputStream(cipher)
        val ephemBytes = ByteArray(2 * ((CURVE.curve.fieldSize + 7) / 8) + 1)

        inputStream.read(ephemBytes)
        val ephem = CURVE.curve.decodePoint(ephemBytes)
        val IV = ByteArray(KEY_SIZE / 8)
        inputStream.read(IV)
        val cipherBody = ByteArray(inputStream.available())
        inputStream.read(cipherBody)

        val iesEngine = makeIESEngine(false, ephem, privKey, IV)

        plaintext = iesEngine.processBlock(cipherBody, 0, cipherBody.size, macData)

        return plaintext
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