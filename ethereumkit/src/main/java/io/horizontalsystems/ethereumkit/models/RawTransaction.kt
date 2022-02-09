package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import java.math.BigInteger

class RawTransaction(
        val gasPrice: GasPrice,
        val gasLimit: Long,
        val to: Address,
        val value: BigInteger,
        val nonce: Long,
        val data: ByteArray = ByteArray(0)
) {

    override fun toString(): String {
        return "RawTransaction [gasPrice: $gasPrice; gasLimit: $gasLimit; to: $to; value: $value; data: ${data.toHexString()}; nonce: $nonce]"
    }
}
