package io.horizontalsystems.ethereumkit.crypto


import org.bouncycastle.crypto.*
import org.bouncycastle.crypto.params.*
import org.bouncycastle.util.Arrays
import org.bouncycastle.util.BigIntegers
import org.bouncycastle.util.Pack
import java.io.ByteArrayInputStream
import java.io.IOException

class IESEngine(var agree: BasicAgreement,
                var kdf: DerivationFunction,
                var mac: Mac, private val hash: Digest, private var cipher: BufferedBlockCipher) {

    private var forEncryption: Boolean = false
    lateinit var privParam: CipherParameters
    lateinit var pubParam: CipherParameters
    lateinit var param: IESParameters

    lateinit var V: ByteArray
    private var keyParser: KeyParser? = null
    private var IV: ByteArray? = null

    fun init(
            forEncryption: Boolean,
            privParam: CipherParameters,
            pubParam: CipherParameters,
            params: CipherParameters) {
        this.forEncryption = forEncryption
        this.privParam = privParam
        this.pubParam = pubParam
        this.V = ByteArray(0)

        extractParams(params)
    }

    private fun extractParams(params: CipherParameters) {
        if (params is ParametersWithIV) {
            this.IV = params.iv
            this.param = params.parameters as IESParameters
        } else {
            this.IV = null
            this.param = params as IESParameters
        }
    }

    @Throws(InvalidCipherTextException::class)
    private fun encryptBlock(
            input: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray?): ByteArray {
        val c = ByteArray(cipher.getOutputSize(inLen))
        lateinit var k: ByteArray
        val k1 = ByteArray((param as IESWithCipherParameters).cipherKeySize / 8)
        val k2 = ByteArray(param.macKeySize / 8)
        var len: Int

        // Block cipher mode.
        k = ByteArray(k1.size + k2.size)

        kdf.generateBytes(k, 0, k.size)
        System.arraycopy(k, 0, k1, 0, k1.size)
        System.arraycopy(k, k1.size, k2, 0, k2.size)

        // If iv provided use it to initialise the cipher
        if (IV != null) {
            cipher.init(true, ParametersWithIV(KeyParameter(k1), IV!!))
        } else {
            cipher.init(true, KeyParameter(k1))
        }

        len = cipher.processBytes(input, inOff, inLen, c, 0)
        len += cipher.doFinal(c, len)

        // Convert the length of the encoding vector into a byte array.
        val P2 = param.encodingV

        // Apply the MAC.
        val T = ByteArray(mac.macSize)
        val K2a = ByteArray(hash.digestSize)

        hash.reset()
        hash.update(k2, 0, k2.size)
        hash.doFinal(K2a, 0)
        mac.init(KeyParameter(K2a))
        mac.update(IV, 0, IV!!.size)
        mac.update(c, 0, c.size)
        if (P2 != null) {
            mac.update(P2, 0, P2.size)
        }
        if (V.isNotEmpty() && P2 != null) {
            val L2 = ByteArray(4)
            Pack.intToBigEndian(P2.size * 8, L2, 0)
            mac.update(L2, 0, L2.size)
        }

        if (macData != null) {
            mac.update(macData, 0, macData.size)
        }

        mac.doFinal(T, 0)

        // Output the triple (V,C,T).
        val output = ByteArray(V.size + len + T.size)
        System.arraycopy(V, 0, output, 0, V.size)
        System.arraycopy(c, 0, output, V.size, len)
        System.arraycopy(T, 0, output, V.size + len, T.size)
        return output
    }

    @Throws(InvalidCipherTextException::class)
    private fun decryptBlock(
            in_enc: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray?): ByteArray {
        val M: ByteArray?
        val K: ByteArray?
        val K1: ByteArray?
        val K2: ByteArray?
        var len: Int

        // Ensure that the length of the input is greater than the MAC in bytes
        if (inLen <= param.macKeySize / 8) {
            throw InvalidCipherTextException("Length of input must be greater than the MAC")
        }

        // Block cipher mode.
        K1 = ByteArray((param as IESWithCipherParameters).cipherKeySize / 8)
        K2 = ByteArray(param.macKeySize / 8)
        K = ByteArray(K1.size + K2.size)

        kdf.generateBytes(K, 0, K.size)
        System.arraycopy(K, 0, K1, 0, K1.size)
        System.arraycopy(K, K1.size, K2, 0, K2.size)

        // If IV provide use it to initialize the cipher
        if (IV != null) {
            cipher.init(false, ParametersWithIV(KeyParameter(K1), IV!!))
        } else {
            cipher.init(false, KeyParameter(K1))
        }

        M = ByteArray(cipher.getOutputSize(inLen - V.size - mac.macSize))
        len = cipher.processBytes(in_enc, inOff + V.size, inLen - V.size - mac.macSize, M, 0)
        len += cipher.doFinal(M, len)


        // Convert the length of the encoding vector into a byte array.
        val P2 = param.encodingV

        // Verify the MAC.
        val end = inOff + inLen
        val T1 = Arrays.copyOfRange(in_enc, end - mac.macSize, end)
        val T2 = ByteArray(T1.size)

        val K2a = ByteArray(hash.digestSize)
        hash.reset()
        hash.update(K2, 0, K2.size)
        hash.doFinal(K2a, 0)
        mac.init(KeyParameter(K2a))
        mac.update(IV, 0, IV!!.size)
        mac.update(in_enc, inOff + V.size, inLen - V.size - T2.size)

        if (P2 != null) {
            mac.update(P2, 0, P2.size)
        }

        if (V.isNotEmpty() && P2 != null) {
            val L2 = ByteArray(4)
            Pack.intToBigEndian(P2.size * 8, L2, 0)
            mac.update(L2, 0, L2.size)
        }

        if (macData != null) {
            mac.update(macData, 0, macData.size)
        }

        mac.doFinal(T2, 0)

        if (!Arrays.constantTimeAreEqual(T1, T2)) {
            throw InvalidCipherTextException("Invalid MAC.")
        }

        // Output the message.
        return Arrays.copyOfRange(M, 0, len)
    }

    @Throws(InvalidCipherTextException::class)
    @JvmOverloads
    fun processBlock(
            input: ByteArray,
            inOff: Int,
            inLen: Int,
            macData: ByteArray? = null): ByteArray {

        if (keyParser != null) {
            val bIn = ByteArrayInputStream(input, inOff, inLen)

            try {
                this.pubParam = keyParser!!.readKey(bIn)
            } catch (e: IOException) {
                throw InvalidCipherTextException("unable to recover ephemeral public key: " + e.message, e)
            }

            val encLength = inLen - bIn.available()
            this.V = Arrays.copyOfRange(input, inOff, inOff + encLength)
        }

        // Compute the common value and convert to byte array.
        agree.init(privParam)
        val z = agree.calculateAgreement(pubParam)
        val Z = BigIntegers.asUnsignedByteArray(agree.fieldSize, z)

        // Initialise the KDF.
        val kdfParam: DerivationParameters
        kdfParam = KDFParameters(Z, param.derivationV)
        kdf.init(kdfParam)

        return if (forEncryption)
            encryptBlock(input, inOff, inLen, macData)
        else
            decryptBlock(input, inOff, inLen, macData)
    }
}
