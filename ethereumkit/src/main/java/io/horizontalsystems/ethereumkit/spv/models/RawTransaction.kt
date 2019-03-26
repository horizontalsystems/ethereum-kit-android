package io.horizontalsystems.ethereumkit.spv.models

import java.math.BigInteger

class RawTransaction(val nonce: BigInteger,
                     val gasPrice: BigInteger,
                     val gasLimit: BigInteger,
                     val to: String,
                     val value: BigInteger,
                     var data: String? = null) {

    override fun toString(): String {
        return "RawTransaction [nonce: $nonce; gasPrice: $gasPrice; gasLimit: $gasLimit; to: $to; value: $value; data: $data]"
    }
}
