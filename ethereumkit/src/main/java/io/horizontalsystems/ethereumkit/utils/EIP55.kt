package io.horizontalsystems.ethereumkit.utils

import io.horizontalsystems.ethereumkit.core.stripHexPrefix
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils

object EIP55 {

    fun encode(data: ByteArray): String {
        return format(data.toHexString())
    }

    fun format(address: String): String {
        val lowercaseAddress = address.stripHexPrefix().toLowerCase()
        val addressHash = CryptoUtils.sha3(lowercaseAddress.toByteArray()).toRawHexString()

        val result = StringBuilder(lowercaseAddress.length + 2)

        result.append("0x")

        for (i in 0 until lowercaseAddress.length) {
            if (Integer.parseInt(addressHash[i].toString(), 16) >= 8) {
                result.append(lowercaseAddress[i].toString().toUpperCase())
            } else {
                result.append(lowercaseAddress[i])
            }
        }

        return result.toString()
    }
}
