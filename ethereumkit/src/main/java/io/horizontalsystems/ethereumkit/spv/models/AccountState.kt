package io.horizontalsystems.ethereumkit.spv.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.room.RoomTypeConverters
import java.math.BigInteger

@Entity
@TypeConverters(RoomTypeConverters::class)
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