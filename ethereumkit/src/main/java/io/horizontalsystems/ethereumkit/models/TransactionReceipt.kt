package io.horizontalsystems.ethereumkit.models

class TransactionReceipt(
        val transactionHash: ByteArray,
        val transactionIndex: Int,
        val blockHash: ByteArray,
        val blockNumber: Long?,
        val from: Address,
        val to: Address?,
        val cumulativeGasUsed: Long,
        val gasUsed: Long,
        val contractAddress: Address?,
        val logs: List<EthereumLog>,
        val logsBloom: ByteArray,
        val root: ByteArray?,
        val status: Int?
)
