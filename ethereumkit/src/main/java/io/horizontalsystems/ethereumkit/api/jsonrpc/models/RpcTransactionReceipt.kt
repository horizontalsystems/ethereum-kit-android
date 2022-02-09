package io.horizontalsystems.ethereumkit.api.jsonrpc.models

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionLog
import io.horizontalsystems.ethereumkit.models.TransactionReceipt

class RpcTransactionReceipt(
        val transactionHash: ByteArray,
        val transactionIndex: Int,
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
        val status: Int?
) {

    constructor(transactionReceipt: TransactionReceipt, logs: List<TransactionLog>) : this(
            transactionReceipt.transactionHash,
            transactionReceipt.transactionIndex,
            transactionReceipt.blockHash,
            transactionReceipt.blockNumber,
            transactionReceipt.from,
            transactionReceipt.to,
            transactionReceipt.effectiveGasPrice,
            transactionReceipt.cumulativeGasUsed,
            transactionReceipt.gasUsed,
            transactionReceipt.contractAddress,
            logs,
            transactionReceipt.logsBloom,
            transactionReceipt.root,
            transactionReceipt.status
    )

}
