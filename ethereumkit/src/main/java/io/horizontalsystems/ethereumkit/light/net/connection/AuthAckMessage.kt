package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils.CURVE
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.rlp.RLPList
import io.horizontalsystems.ethereumkit.light.toInt
import org.spongycastle.math.ec.ECPoint

class AuthAckMessage(val publicKeyPoint: ECPoint, val nonce: ByteArray, val version: Int) {

    companion object {
        fun decode(wire: ByteArray): AuthAckMessage {
            val params = RLP.decode2OneItem(wire, 0) as RLPList
            val pubKeyBytes = params[0].rlpData ?: ByteArray(65) { 0 }
            val bytes = ByteArray(65)
            System.arraycopy(pubKeyBytes, 0, bytes, 1, 64)
            bytes[0] = 0x04
            val ephemeralPublicKey = CURVE.curve.decodePoint(bytes)
            val nonce = params[1].rlpData
            val versionBytes = params[2].rlpData
            val version = versionBytes.toInt()

            return AuthAckMessage(ephemeralPublicKey, nonce ?: byteArrayOf(0), version)
        }
    }

    override fun toString(): String {
        return "AuthAckMessage [publicKeyPoint: $publicKeyPoint; nonce: ${nonce.toHexString()}; version: $version]"
    }
}