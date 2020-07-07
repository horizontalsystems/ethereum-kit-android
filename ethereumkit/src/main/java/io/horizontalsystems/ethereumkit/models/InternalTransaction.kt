package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity(foreignKeys = [ForeignKey(
        entity = EthereumTransaction::class,
        parentColumns = ["hash"],
        childColumns = ["hash"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE,
        deferred = true)
])
class InternalTransaction(
        val hash: ByteArray,
        val blockNumber: Long,
        val from: ByteArray,
        val to: ByteArray,
        val value: BigInteger,
        val traceId: Int,
        @PrimaryKey(autoGenerate = true) val id: Long = 0
)
