package io.horizontalsystems.ethereumkit.crypto

import io.horizontalsystems.ethereumkit.spv.core.toBytes
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import org.bouncycastle.asn1.sec.SECNamedCurves
import org.bouncycastle.asn1.x9.X9IntegerConverter
import org.bouncycastle.crypto.BufferedBlockCipher
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.modes.SICBlockCipher
import org.bouncycastle.crypto.params.*
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.crypto.signers.HMacDSAKCalculator
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECAlgorithms
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.bouncycastle.util.BigIntegers
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

object CryptoUtils {

    val CURVE: ECDomainParameters
    val HALF_CURVE_ORDER: BigInteger
    private val CURVE_SPEC: ECParameterSpec

    private val HASH_256_ALGORITHM_NAME: String = "ETH-KECCAK-256"

    const val SECRET_SIZE = 32
    private const val KEY_SIZE = 128
    private val ECIES_PREFIX_SIZE = 65 + KEY_SIZE / 8 + 32 // 256 bit EC public key, IV, 256 bit MAC

    init {
        val params = SECNamedCurves.getByName("secp256k1")
        CURVE = ECDomainParameters(params.curve, params.g, params.n, params.h)
        CURVE_SPEC = ECParameterSpec(params.curve, params.g, params.n, params.h)
        HALF_CURVE_ORDER = params.n.shiftRight(1)
    }

    fun ecdhAgree(myKey: ECKey, remotePublicKeyPoint: ECPoint): ByteArray {
        val agreement = ECDHBasicAgreement()
        agreement.init(ECPrivateKeyParameters(myKey.privateKey, CURVE))
        return agreement.calculateAgreement(ECPublicKeyParameters(remotePublicKeyPoint, CURVE)).toBytes(SECRET_SIZE)
    }

    fun ellipticSign(messageToSign: ByteArray, privateKey: BigInteger): ByteArray {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters(privateKey, CURVE)
        signer.init(true, privKeyParams)
        val components = signer.generateSignature(messageToSign)

        val r = components[0]
        var s = components[1]

        //canonicalize s
        s = if (s <= HALF_CURVE_ORDER) s else CURVE.n.subtract(s)

        var recId = -1
        val thisKey = CURVE.g.multiply(privateKey).getEncoded(false)
        for (i in 0..3) {
            val k = recoverPubBytesFromSignature(i, r, s, messageToSign)
            if (k != null && Arrays.equals(k, thisKey)) {
                recId = i
                break
            }
        }
        val rsigPad = ByteArray(32)
        val rsig = BigIntegers.asUnsignedByteArray(r)
        System.arraycopy(rsig, 0, rsigPad, rsigPad.size - rsig.size, rsig.size)

        val ssigPad = ByteArray(32)
        val ssig = BigIntegers.asUnsignedByteArray(s)
        System.arraycopy(ssig, 0, ssigPad, ssigPad.size - ssig.size, ssig.size)

        return rsigPad + ssigPad + byteArrayOf(recId.toByte())
    }

    fun eciesDecrypt(privateKey: BigInteger, message: ECIESEncryptedMessage): ByteArray {
        val ephem = CURVE.curve.decodePoint(message.ephemeralPublicKey)

        val iesEngine = makeIESEngine(false, ephem, privateKey, message.initialVector)

        val cipherBody = message.cipher + message.checkSum

        return iesEngine.processBlock(cipherBody, 0, cipherBody.size, message.prefixBytes)
    }

    fun eciesEncrypt(remotePublicKey: ECPoint, message: ByteArray): ECIESEncryptedMessage {
        val size = message.size + ECIES_PREFIX_SIZE
        val prefixBytes = size.toShort().toBytes()

        val iv = RandomHelper.randomBytes(KEY_SIZE / 8)

        val ephemPair = RandomHelper.randomECKey()
        val iesEngine = makeIESEngine(true, remotePublicKey, ephemPair.privateKey, iv)

        val cipher = iesEngine.processBlock(message, 0, message.size, prefixBytes)

        return ECIESEncryptedMessage(prefixBytes,
                ephemPair.publicKeyPoint.getEncoded(false),
                iv,
                cipher.copyOfRange(0, cipher.size - 32),
                cipher.copyOfRange(cipher.size - 32, cipher.size))
    }

    fun sha3(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME)
        digest.update(data)
        return digest.digest()
    }

    fun aesEncrypt(key: ByteArray, data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        val aesEngine = AESEngine()
        aesEngine.init(true, KeyParameter(key))
        aesEngine.processBlock(data, 0, result, 0)
        return result
    }

    fun ecKeyFromPrivate(privKey: BigInteger): ECKey {
        return ECKey(privKey, CURVE.g.multiply(privKey))
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

    private fun recoverPubBytesFromSignature(recId: Int, r: BigInteger, s: BigInteger, messageHash: ByteArray?): ByteArray? {
        val n = CURVE.n
        val i = BigInteger.valueOf(recId.toLong() / 2)
        val x = r.add(i.multiply(n))
        val curve = CURVE.curve as ECCurve.Fp
        val prime = curve.q
        if (x >= prime) {
            return null
        }
        val R = decompressKey(x, recId and 1 == 1)

        if (!R.multiply(n).isInfinity)
            return null
        val e = BigInteger(1, messageHash)

        val eInv = BigInteger.ZERO.subtract(e).mod(n)
        val rInv = r.modInverse(n)
        val srInv = rInv.multiply(s).mod(n)
        val eInvrInv = rInv.multiply(eInv).mod(n)
        val q = ECAlgorithms.sumOfTwoMultiplies(CURVE.g, eInvrInv, R, srInv) as ECPoint.Fp

        return if (q.isInfinity) null else q.getEncoded(false)
    }

    private fun decompressKey(xBN: BigInteger, yBit: Boolean): ECPoint {
        val x9 = X9IntegerConverter()
        val compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(CURVE.curve))
        compEnc[0] = (if (yBit) 0x03 else 0x02).toByte()
        return CURVE.curve.decodePoint(compEnc)
    }
}
