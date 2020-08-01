package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toHexString

class TransactionInfo(transactionWithInternal: TransactionWithInternal) {
    val hash: String
    val nonce: Long
    val input: String
    val from: String
    val to: String
    val value: String
    val gasLimit: Long
    val gasPrice: Long
    val timestamp: Long

    val blockHash: String?
    val blockNumber: Long?
    val gasUsed: Long?
    val cumulativeGasUsed: Long?
    val isError: Int?
    val transactionIndex: Int?
    val txReceiptStatus: Int?

    val internalTransactions: List<InternalTransactionInfo> = transactionWithInternal.internalTransactions.map { InternalTransactionInfo(it) }

    init {
        val transaction = transactionWithInternal.transaction

        hash = transaction.hash.toHexString()
        nonce = transaction.nonce
        input = transaction.input.toHexString()
        from = transaction.from.eip55
        to = transaction.to.eip55
        value = transaction.value.toString()
        gasLimit = transaction.gasLimit
        gasPrice = transaction.gasPrice
        timestamp = transaction.timestamp

        blockHash = transaction.blockHash?.toHexString()
        blockNumber = transaction.blockNumber
        gasUsed = transaction.gasUsed
        cumulativeGasUsed = transaction.cumulativeGasUsed
        isError = transaction.iserror
        transactionIndex = transaction.transactionIndex
        txReceiptStatus = transaction.txReceiptStatus
    }

}
