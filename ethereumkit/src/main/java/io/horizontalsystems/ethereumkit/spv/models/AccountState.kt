package io.horizontalsystems.ethereumkit.spv.models

import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger

class AccountState(val address: ByteArray, val nonce: Long, val balance: BigInteger, val storageHash: ByteArray, val codeHash: ByteArray) {

    override fun toString(): String {
        return "(\n" +
                "  nonce: $nonce\n" +
                "  balance: $balance\n" +
                "  storageHash: ${storageHash.toHexString()}\n" +
                "  codeHash: ${codeHash.toHexString()}\n" +
                ")"
    }
}