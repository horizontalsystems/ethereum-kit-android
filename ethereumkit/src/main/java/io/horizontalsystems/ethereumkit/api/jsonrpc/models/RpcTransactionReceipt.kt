package io.horizontalsystems.ethereumkit.api.jsonrpc.models

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionLog

class RpcTransactionReceipt(
        val transactionHash: ByteArray,
        val transactionIndex: Long,
        val blockHash: ByteArray,
        val blockNumber: Long,
        val from: Address,
        val to: Address?,
        val effectiveGasPrice: Long,
        val cumulativeGasUsed: Long,
        val gasUsed: Long,
        val contractAddress: Address?,
        val logs: List<TransactionLog>,
        val logsBloom: ByteArray,
        val root: ByteArray?,
        val status: Long?
)
