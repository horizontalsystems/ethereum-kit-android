package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toHexString

class InternalTransactionInfo(transaction: InternalTransaction) {
    val hash: String = transaction.hash.toHexString()
    val blockNumber: Long = transaction.blockNumber
    val from: String = transaction.from.eip55
    val to: String = transaction.to.eip55
    val value: String = transaction.value.toString()
    val traceId: Int = transaction.traceId
}
