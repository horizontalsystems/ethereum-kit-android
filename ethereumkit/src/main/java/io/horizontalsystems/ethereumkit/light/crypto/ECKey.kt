package io.horizontalsystems.ethereumkit.light.crypto

import io.horizontalsystems.ethereumkit.core.toHexString
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger

class ECKey(val privateKey: BigInteger, val publicKeyPoint: ECPoint) {
    override fun toString(): String {
        return "ECKey [privateKey: ${privateKey.toByteArray()?.toHexString()}; publicKey: $publicKeyPoint"
    }
}