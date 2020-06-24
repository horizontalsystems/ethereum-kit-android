package io.horizontalsystems.ethereumkit.crypto

import org.bouncycastle.crypto.DataLengthException
import org.bouncycastle.crypto.DerivationParameters
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.DigestDerivationFunction
import org.bouncycastle.crypto.params.ISO18033KDFParameters
import org.bouncycastle.crypto.params.KDFParameters
import org.bouncycastle.util.Pack

/**
 * Basic KDF generator for derived keys and ivs as defined by NIST SP 800-56A.
 */
/**
 * Construct a KDF Parameters generator.
 *
 *
 *
 * @param counterStart
 * value of counter.
 * @param digest
 * the digest to be used as the source of derived keys.
 */
class ConcatKDFBytesGenerator(private val counterStart: Int, private val digest: Digest) : DigestDerivationFunction {
    private var shared: ByteArray? = null
    private var iv: ByteArray? = null

    constructor(digest: Digest) : this(1, digest)

    override fun init(param: DerivationParameters) {
        when (param) {
            is KDFParameters -> {
                shared = param.sharedSecret
                iv = param.iv
            }
            is ISO18033KDFParameters -> {
                shared = param.seed
                iv = null
            }
            else -> {
                throw IllegalArgumentException("KDF parameters required for KDF2Generator")
            }
        }
    }

    /**
     * return the underlying digest.
     */
    override fun getDigest(): Digest {
        return digest
    }

    /**
     * fill len bytes of the output buffer with bytes generated from the
     * derivation function.
     *
     * @throws IllegalArgumentException
     * if the size of the request will cause an overflow.
     * @throws DataLengthException
     * if the out buffer is too small.
     */
    @Throws(DataLengthException::class, IllegalArgumentException::class)
    override fun generateBytes(out: ByteArray, outOff: Int, len: Int): Int {
        var mOutOff = outOff
        var mLen = len
        if (out.size - mLen < mOutOff) {
            throw DataLengthException("output buffer too small")
        }

        val oBytes = mLen.toLong()
        val outLen = digest.digestSize

        //
        // this is at odds with the standard implementation, the
        // maximum value should be hBits * (2^32 - 1) where hBits
        // is the digest output size in bits. We can't have an
        // array with a long index at the moment...
        //
        if (oBytes > (2L shl 32) - 1) {
            throw IllegalArgumentException("Output length too large")
        }

        val cThreshold = ((oBytes + outLen - 1) / outLen).toInt()

        val dig = ByteArray(digest.digestSize)

        val C = ByteArray(4)
        Pack.intToBigEndian(counterStart, C, 0)

        var counterBase = counterStart and 0xFF.inv()

        for (i in 0 until cThreshold) {
            digest.update(C, 0, C.size)
            digest.update(shared, 0, shared!!.size)

            if (iv != null) {
                digest.update(iv, 0, iv!!.size)
            }

            digest.doFinal(dig, 0)

            if (mLen > outLen) {
                System.arraycopy(dig, 0, out, mOutOff, outLen)
                mOutOff += outLen
                mLen -= outLen
            } else {
                System.arraycopy(dig, 0, out, mOutOff, mLen)
            }

            if ((++C[3]).toInt() == 0) {
                counterBase += 0x100
                Pack.intToBigEndian(counterBase, C, 0)
            }
        }

        digest.reset()

        return oBytes.toInt()
    }
}
