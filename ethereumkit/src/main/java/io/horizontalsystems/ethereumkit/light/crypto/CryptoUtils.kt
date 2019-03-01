package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.light.RandomUtils
import io.horizontalsystems.ethereumkit.light.toBytes
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.BufferedBlockCipher
import org.spongycastle.crypto.agreement.ECDHBasicAgreement
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.macs.HMac
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.*
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.signers.HMacDSAKCalculator
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.jce.spec.ECParameterSpec
import org.spongycastle.jce.spec.ECPrivateKeySpec
import org.spongycastle.math.ec.ECAlgorithms
import org.spongycastle.math.ec.ECCurve
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.util.BigIntegers
import java.math.BigInteger
import java.security.*
import java.util.*

object CryptoUtils {

    val CURVE: ECDomainParameters
    private val CURVE_SPEC: ECParameterSpec

    private val CRYPTO_PROVIDER: Provider
    private val HASH_256_ALGORITHM_NAME: String

    const val SECRET_SIZE = 32
    private val KEY_SIZE = 128
    private val ECIES_PREFIX_SIZE = 65 + KEY_SIZE / 8 + 32 // 256 bit EC public key, IV, 256 bit MAC

    init {
        val params = SECNamedCurves.getByName("secp256k1")
        CURVE = ECDomainParameters(params.curve, params.g, params.n, params.h)
        CURVE_SPEC = ECParameterSpec(params.curve, params.g, params.n, params.h)

        Security.addProvider(SpongyCastleProvider.getInstance())
        CRYPTO_PROVIDER = Security.getProvider("SC")
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256"
    }

    fun ecdhAgree(myKey: ECKey, remotePublicKeyPoint: ECPoint): ByteArray {
        val agreement = ECDHBasicAgreement()
        agreement.init(ECPrivateKeyParameters((privateKeyFromBigInteger(myKey.privateKey) as BCECPrivateKey).d, CURVE))
        return agreement.calculateAgreement(ECPublicKeyParameters(remotePublicKeyPoint, CURVE)).toBytes(SECRET_SIZE)
    }

    fun ellipticSign(messageToSign: ByteArray, key: ECKey): ByteArray {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters((privateKeyFromBigInteger(key.privateKey) as BCECPrivateKey).d, CURVE)
        signer.init(true, privKeyParams)
        val components = signer.generateSignature(messageToSign)

        val r = components[0]
        val s = components[1]

        var recId = -1
        val thisKey = CURVE.g.multiply(key.privateKey).getEncoded(false)
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

        val sigBytes = rsigPad + ssigPad + byteArrayOf(recId.toByte())

        return sigBytes
    }

    fun eciesDecrypt(privateKey: BigInteger, message: ECIESEncryptedMessage): ByteArray {
        val ephem = CURVE.curve.decodePoint(message.ephemeralPubKey)

        val iesEngine = makeIESEngine(false, ephem, privateKey, message.initialVector)

        val cipherBody = message.cipher + message.checkSum

        return iesEngine.processBlock(cipherBody, 0, cipherBody.size, message.prefixBytes)
    }

    fun eciesEncrypt(remotePublicKey: ECPoint, message: ByteArray): ECIESEncryptedMessage {
        val size = message.size + ECIES_PREFIX_SIZE
        val prefixBytes = size.toShort().toBytes()

        val iv = ByteArray(KEY_SIZE / 8)
        SecureRandom().nextBytes(iv)

        val ephemPair = RandomUtils.randomECKey()
        val iesEngine = makeIESEngine(true, remotePublicKey, ephemPair.privateKey, iv)

        val cipher = iesEngine.processBlock(message, 0, message.size, prefixBytes)

        return ECIESEncryptedMessage(prefixBytes,
                ephemPair.publicKeyPoint.getEncoded(false),
                iv,
                cipher.copyOfRange(0, cipher.size - 32),
                cipher.copyOfRange(cipher.size - 32, cipher.size))
    }

    fun sha3(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
        digest.update(data)
        return digest.digest()
    }

    fun encryptAES(key: ByteArray, data: ByteArray): ByteArray {
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

    private fun privateKeyFromBigInteger(priv: BigInteger): PrivateKey {
        return KeyFactory.getInstance("EC", SpongyCastleProvider.getInstance())
                .generatePrivate(ECPrivateKeySpec(priv, CURVE_SPEC))
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
