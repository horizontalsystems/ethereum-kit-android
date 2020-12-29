package io.horizontalsystems.erc20kit.models

import androidx.room.Entity
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger
import java.util.*

enum class TransactionType(val value: String) {
    TRANSFER("transfer"),
    APPROVE("approve");

    companion object {
        fun valueOf(value: String?): TransactionType? {
            return values().find { it.value == value }
        }
    }
}

@Entity(primaryKeys = ["hash", "interTransactionIndex"])
data class TransactionRecord(
        val hash: ByteArray,
        var interTransactionIndex: Int = 0,
        var logIndex: Int?,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        var timestamp: Long,
        var type: TransactionType
) {

    override fun equals(other: Any?): Boolean {
        if (other !is TransactionRecord)
            return false

        return hash.contentEquals(other.hash) && interTransactionIndex == other.interTransactionIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(hash, interTransactionIndex)
    }
}
