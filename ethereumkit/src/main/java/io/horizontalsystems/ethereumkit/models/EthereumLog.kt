package io.horizontalsystems.ethereumkit.models

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

        var timestamp: Long?)