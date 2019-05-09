package io.horizontalsystems.ethereumkit.spv.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger

@Entity
class AccountState(@PrimaryKey
                   val address: ByteArray,
                   val nonce: Long,
                   val balance: BigInteger,
                   val storageHash: ByteArray,
                   val codeHash: ByteArray) {

    override fun toString(): String {
        return "(\n" +
                "  nonce: $nonce\n" +
                "  balance: $balance\n" +
                "  storageHash: ${storageHash.toHexString()}\n" +
                "  codeHash: ${codeHash.toHexString()}\n" +
                ")"
    }
}