package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.core.toHexString

class TransactionInfo(transaction: Transaction) {

    val transactionHash: String
    val transactionIndex: Int?
    val interTransactionIndex: Int
    val from: String
    val to: String
    val value: String
    val timestamp: Long

    var logIndex: Int? = null
    var blockHash: String? = null
    var blockNumber: Long? = null
    var isError: Boolean

    init {
        transactionHash = transaction.transactionHash.toHexString()
        transactionIndex = transaction.transactionIndex
        interTransactionIndex = transaction.interTransactionIndex
        logIndex = transaction.logIndex
        from = transaction.from.eip55
        to = transaction.to.eip55
        value = transaction.value.toString(10)
        timestamp = transaction.timestamp
        blockHash = transaction.blockHash?.toHexString()
        blockNumber = transaction.blockNumber
        isError = transaction.isError
    }

}
