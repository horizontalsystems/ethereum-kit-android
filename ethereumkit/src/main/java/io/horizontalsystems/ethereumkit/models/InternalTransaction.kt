package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.math.BigInteger
import java.util.*

@Entity(foreignKeys = [ForeignKey(
        entity = Transaction::class,
        parentColumns = ["hash"],
        childColumns = ["hash"],
        onDelete = ForeignKey.CASCADE
)
])
data class InternalTransaction(
        val hash: ByteArray,
        val blockNumber: Long,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        val traceId: Int,
        @PrimaryKey(autoGenerate = true) val id: Long = 0
) {

    override fun equals(other: Any?): Boolean {
        if (other !is InternalTransaction)
            return false

        return hash.contentEquals(other.hash) && id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, id)
    }
}

