package io.horizontalsystems.ethereumkit.spv.rlp

import org.bouncycastle.util.encoders.Hex
import java.io.Serializable

class DecodeResult(val pos: Int, val decoded: Any) : Serializable {

    override fun toString(): String {
        return asString(this.decoded)
    }

    private fun asString(decoded: Any?): String = when (decoded) {
        is String -> decoded
        is ByteArray -> Hex.toHexString(decoded)
        is Array<*> -> {
            val result = StringBuilder()
            for (item in decoded) {
                result.append(asString(item))
            }
            result.toString()
        }
        else -> ""
    }
}