package io.horizontalsystems.ethereumkit.sample.core

import java.math.BigDecimal

class TransactionRecord (
    val transactionHash: String,
    val blockHeight: Long?,
    val amount: BigDecimal,
    val timestamp: Long,
    var from: TransactionAddress,
    var to: TransactionAddress)

class TransactionAddress (val address: String, val mine: Boolean)
