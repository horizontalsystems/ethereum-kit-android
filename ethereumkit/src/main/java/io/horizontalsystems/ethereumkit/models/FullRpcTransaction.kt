package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt

data class FullRpcTransaction(
    val rpcTransaction: RpcTransaction,
    val rpcReceipt: RpcTransactionReceipt?,
    val rpcBlock: RpcBlock?,
    var internalTransactions: MutableList<InternalTransaction> = mutableListOf()
) {

    val isFailed: Boolean =
        when {
            rpcReceipt == null -> false
            rpcReceipt.status == null -> rpcTransaction.gasLimit == rpcReceipt.cumulativeGasUsed
            else -> rpcReceipt.status == 0L
        }

    fun transaction(timestamp: Long) =
        Transaction(
            rpcTransaction.hash,
            timestamp,
            isFailed,
            rpcBlock?.number,
            rpcReceipt?.transactionIndex?.toInt(),
            rpcTransaction.from,
            rpcTransaction.to,
            rpcTransaction.value,
            rpcTransaction.input,
            rpcTransaction.nonce,
            rpcTransaction.gasPrice,
            rpcTransaction.maxFeePerGas,
            rpcTransaction.maxPriorityFeePerGas,
            rpcTransaction.gasLimit,
            rpcReceipt?.gasUsed
        )

}
