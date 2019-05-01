package io.horizontalsystems.ethereumkit.models

import java.util.*

data class EthereumLog(
        val address: String,
        val blockHash: String,
        val blockNumber: Long,
        val data: String,
        val logIndex: Int,
        val removed: Boolean,
        val topics: List<String>,
        val transactionHash: String,
        val transactionIndex: Int,

        var timestamp: Long?) {

    override fun equals(other: Any?): Boolean {
        if (other !is EthereumLog)
            return false

        return transactionHash == other.transactionHash && logIndex == other.logIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(transactionHash, logIndex)
    }
}
