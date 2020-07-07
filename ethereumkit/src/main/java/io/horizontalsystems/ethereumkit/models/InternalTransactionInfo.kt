package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.core.toEIP55Address
import io.horizontalsystems.ethereumkit.core.toHexString

class InternalTransactionInfo(transaction: InternalTransaction) {
    val hash: String = transaction.hash.toHexString()
    val blockNumber: Long = transaction.blockNumber
    val from: String = transaction.from.toEIP55Address()
    val to: String = transaction.to.toEIP55Address()
    val value: String = transaction.value.toString()
    val traceId: Int = transaction.traceId
}
