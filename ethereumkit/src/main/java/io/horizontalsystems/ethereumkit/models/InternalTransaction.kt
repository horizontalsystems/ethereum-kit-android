package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger
import java.util.*

@Entity
data class InternalTransaction(
        val hash: ByteArray,
        val blockNumber: Long,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        @PrimaryKey(autoGenerate = true) val id: Long = 0
) {

    @delegate:Ignore
    val hashString: String by lazy {
        hash.toHexString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is InternalTransaction)
            return false

        return hash.contentEquals(other.hash) && id == other.id
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, id)
    }

}
