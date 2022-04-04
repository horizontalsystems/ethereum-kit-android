package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt

data class FullRpcTransaction(
    val rpcTransaction: RpcTransaction,
    val rpcReceipt: RpcTransactionReceipt,
    var internalTransactions: MutableList<InternalTransaction> = mutableListOf(),
    val rpcBlock: RpcBlock
) {

    val isFailed: Boolean =
        if (rpcReceipt.status == null) rpcTransaction.gasLimit == rpcReceipt.cumulativeGasUsed else rpcReceipt.status == 0

    val transaction: Transaction =
        Transaction(
            rpcTransaction.hash,
            rpcBlock.timestamp,
            isFailed,
            rpcBlock.number,
            rpcReceipt.transactionIndex,
            rpcTransaction.from,
            rpcTransaction.to,
            rpcTransaction.value,
            rpcTransaction.input,
            rpcTransaction.nonce,
            rpcTransaction.gasPrice,
            rpcTransaction.maxFeePerGas,
            rpcTransaction.maxPriorityFeePerGas,
            rpcTransaction.gasLimit,
            rpcReceipt.gasUsed
    )

}
