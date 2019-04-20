package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.core.toHexString

class TransactionInfo {

    val transactionHash: String
    val from: String
    val to: String
    val value: String
    val timestamp: Long

    var logIndex: Int? = null
    var blockHash: String? = null
    var blockNumber: Long? = null

    constructor(transaction: Transaction) {
        transactionHash = transaction.transactionHash.toHexString()
        logIndex = transaction.logIndex
        from = transaction.from.toHexString()
        to = transaction.to.toHexString()
        value = transaction.value.toString(10)
        timestamp = transaction.timestamp
        blockHash = transaction.blockHash?.toHexString()
        blockNumber = transaction.blockNumber
    }

    constructor(hash: String, from: String, to: String, value: String, timestamp: Long) {
        this.transactionHash = hash
        this.from = from
        this.to = to
        this.value = value
        this.timestamp = timestamp
    }

}
