package io.horizontalsystems.ethereumkit.crypto

import io.horizontalsystems.ethereumkit.core.toHexString
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger


class ECKey(val privateKey: BigInteger, val publicKeyPoint: ECPoint) {
    override fun toString(): String {
        return "ECKey [privateKey: ${privateKey.toByteArray()?.toHexString()}; publicKey: $publicKeyPoint"
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ECKey)
            return false

        return this.privateKey == other.privateKey && this.publicKeyPoint.equals(other.publicKeyPoint)
    }

    override fun hashCode(): Int {
        var result = privateKey.hashCode()
        result = 31 * result + publicKeyPoint.hashCode()
        return result
    }
}