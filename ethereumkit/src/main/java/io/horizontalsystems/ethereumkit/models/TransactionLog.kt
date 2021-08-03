package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import java.util.*

@Entity(foreignKeys = [ForeignKey(
        entity = TransactionReceipt::class,
        parentColumns = ["transactionHash"],
        childColumns = ["transactionHash"],
        onDelete = ForeignKey.CASCADE
)], primaryKeys = ["transactionHash", "logIndex"])
data class TransactionLog(
        val transactionHash: ByteArray,
        val transactionIndex: Int,
        val logIndex: Int,
        val address: Address,
        val blockHash: ByteArray,
        val blockNumber: Long,
        val data: ByteArray,
        val removed: Boolean,
        val topics: List<String>,
        var timestamp: Long? = null
) {

    @Ignore
    var relevant: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (other !is TransactionLog)
            return false

        return transactionHash.contentEquals(other.transactionHash) && logIndex == other.logIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(transactionHash, logIndex)
    }
}
