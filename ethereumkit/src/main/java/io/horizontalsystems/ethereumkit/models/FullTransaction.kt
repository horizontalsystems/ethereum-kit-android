package io.horizontalsystems.ethereumkit.models

class FullTransaction(
        val transaction: Transaction,
        val receiptWithLogs: TransactionReceiptWithLogs?,
        val internalTransactions: List<InternalTransaction>
) {
    val isFailed: Boolean
        get() {
            val receipt = receiptWithLogs?.receipt
            return when {
                receipt == null -> false
                receipt.status == null -> transaction.gasLimit == receipt.cumulativeGasUsed
                else -> receipt.status != 0
            }
        }
}