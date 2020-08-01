package io.horizontalsystems.ethereumkit.spv.models

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class RawTransaction(val gasPrice: Long,
                     val gasLimit: Long,
                     val to: Address,
                     val value: BigInteger,
                     var data: ByteArray = ByteArray(0)) {

    override fun toString(): String {
        return "RawTransaction [gasPrice: $gasPrice; gasLimit: $gasLimit; to: $to; value: $value; data: ${data.toHexString()}]"
    }
}
