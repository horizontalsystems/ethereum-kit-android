package io.horizontalsystems.ethereumkit.api.models.etherscan

data class EtherscanTransaction(
        val blockNumber: String,
        val timestamp: String,
        val hash: String,
        val nonce: String,
        val blockHash: String,
        val transactionIndex: String,
        val from: String,
        val to: String,
        val value: String,
        val gas: String,
        val gasPrice: String,
        val isError: String?,
        val txreceipt_status: String?,
        val input: String,
        val contractAddress: String,
        val cumulativeGasUsed: String,
        val gasUsed: String,
        val confirmations: String
)
