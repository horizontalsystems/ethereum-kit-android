package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toEIP55Address
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger

class TransactionInfo(transaction: EthereumTransaction) {
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

    val contractAddress: String?

    init {
        hash = transaction.hash.toHexString()
        nonce = transaction.nonce
        input = transaction.input.toHexString()
        from = transaction.from.toEIP55Address()
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
        val data = transaction.input
        if (data.count() == 68 && data.copyOfRange(0, 4).toRawHexString() == "a9059cbb") {
            to = data.copyOfRange(4, 36).toEIP55Address()
            value = data.copyOfRange(36, 68).toBigInteger().toString(10)
            contractAddress = transaction.to.toEIP55Address()
        } else {
            to = transaction.to.toEIP55Address()
            value = transaction.value.toString(10)
            contractAddress = null
        }
    }
}
