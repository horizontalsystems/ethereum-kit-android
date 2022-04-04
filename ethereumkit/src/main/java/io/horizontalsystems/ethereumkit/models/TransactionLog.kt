package io.horizontalsystems.ethereumkit.models

import java.util.*

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

    override fun equals(other: Any?): Boolean {
        if (other !is TransactionLog)
            return false

        return transactionHash.contentEquals(other.transactionHash) && logIndex == other.logIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(transactionHash, logIndex)
    }
}
