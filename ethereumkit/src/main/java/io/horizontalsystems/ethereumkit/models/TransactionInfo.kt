package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toEIP55Address
import io.horizontalsystems.ethereumkit.core.toHexString

class TransactionInfo(transaction: EthereumTransaction) {
    val hash: String = transaction.hash.toHexString()
    val nonce: Long = transaction.nonce
    val input: String = transaction.input.toHexString()
    val from: String = transaction.from.toEIP55Address()
    val to: String = transaction.to.toEIP55Address()
    val value: String = transaction.value.toString()
    val gasLimit: Long = transaction.gasLimit
    val gasPrice: Long = transaction.gasPrice
    val timestamp: Long = transaction.timestamp

    val blockHash: String? = transaction.blockHash?.toHexString()
    val blockNumber: Long? = transaction.blockNumber
    val gasUsed: Long? = transaction.gasUsed
    val cumulativeGasUsed: Long? = transaction.cumulativeGasUsed
    val isError: Int? = transaction.iserror
    val transactionIndex: Int? = transaction.transactionIndex
    val txReceiptStatus: Int? = transaction.txReceiptStatus
}
