package io.horizontalsystems.ethereumkit.spv.models

import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger

class RawTransaction(val gasPrice: Long,
                     val gasLimit: Long,
                     val to: ByteArray,
                     val value: BigInteger,
                     var data: ByteArray = ByteArray(0)) {

    override fun toString(): String {
        return "RawTransaction [gasPrice: $gasPrice; gasLimit: $gasLimit; to: $to; value: $value; data: ${data.toHexString()}]"
    }
}
