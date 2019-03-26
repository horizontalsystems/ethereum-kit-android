package io.horizontalsystems.ethereumkit.spv.models

import java.math.BigInteger

class Signature(val v: Byte,
                val r: ByteArray,
                val s: ByteArray) {
    override fun toString(): String {
        return "Signature [v: $v; r: ${BigInteger(r)}; s: ${BigInteger(s)}]"
    }
}
