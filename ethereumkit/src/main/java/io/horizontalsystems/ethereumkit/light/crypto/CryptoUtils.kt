package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.light.ByteUtils
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.asn1.x9.X9IntegerConverter
import org.spongycastle.crypto.agreement.ECDHBasicAgreement
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.engines.AESEngine
import org.spongycastle.crypto.generators.ECKeyPairGenerator
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

    init {
        val params = SECNamedCurves.getByName("secp256k1")
        CURVE = ECDomainParameters(params.curve, params.g, params.n, params.h)
        CURVE_SPEC = ECParameterSpec(params.curve, params.g, params.n, params.h)

        Security.addProvider(SpongyCastleProvider.getInstance())
        CRYPTO_PROVIDER = Security.getProvider("SC")
        HASH_256_ALGORITHM_NAME = "ETH-KECCAK-256"
    }

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

    fun ecKeyFromPrivate(privKey: BigInteger): ECKey {
        return ECKey(privKey, CURVE.g.multiply(privKey))
    }

    fun keyAgreement(privateKey: BigInteger, publicKeyPoint: ECPoint): BigInteger {
        val agreement = ECDHBasicAgreement()
        agreement.init(ECPrivateKeyParameters((CryptoUtils.privateKeyFromBigInteger(privateKey) as BCECPrivateKey).d, CURVE))
        return agreement.calculateAgreement(ECPublicKeyParameters(publicKeyPoint, CURVE))
    }

    fun sign(privateKey: BigInteger, input: ByteArray): ByteArray {
        val signer = ECDSASigner(HMacDSAKCalculator(SHA256Digest()))
        val privKeyParams = ECPrivateKeyParameters((privateKeyFromBigInteger(privateKey) as BCECPrivateKey).d, CURVE)
        signer.init(true, privKeyParams)
        val components = signer.generateSignature(input)

        val r = components[0]
        val s = components[1]

        var recId = -1
        val thisKey = CURVE.g.multiply(privateKey).getEncoded(false)
        for (i in 0..3) {
            val k = recoverPubBytesFromSignature(i, r, s, input)
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

        val sigBytes = ByteUtils.merge(rsigPad, ssigPad, byteArrayOf(recId.toByte()))

        return sigBytes
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

    fun encryptAES(key: ByteArray, data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        val aesEngine = AESEngine()
        aesEngine.init(true, KeyParameter(key))
        aesEngine.processBlock(data, 0, result, 0)
        return result
    }

    //-----------------Hash Utils------------------------------

    fun sha3(input1: ByteArray, input2: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input1, 0, input1.size)
            digest.update(input2, 0, input2.size)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }

    fun sha3(input: ByteArray): ByteArray {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance(HASH_256_ALGORITHM_NAME, CRYPTO_PROVIDER)
            digest.update(input)
            return digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }

    }
}