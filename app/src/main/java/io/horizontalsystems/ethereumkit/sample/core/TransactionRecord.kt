package io.horizontalsystems.ethereumkit.sample.core

import java.math.BigDecimal

class TransactionRecord(
        val transactionHash: String,
        val index: Int,
        val amount: BigDecimal,
        val timestamp: Long,

        var from: TransactionAddress,
        var to: TransactionAddress,

        val blockHeight: Long?)

class TransactionAddress(val address: String, val mine: Boolean)
