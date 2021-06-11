package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import java.math.BigDecimal

class TransactionRecord(
        val transactionHash: String,
        val transactionIndex: Int,
        val interTransactionIndex: Int,
        val amount: BigDecimal,
        val timestamp: Long,

        var from: TransactionAddress,
        var to: TransactionAddress,

        val blockHeight: Long?,
        val isError: Boolean,
        val type: String = "",
        val mainDecoration: TransactionDecoration?,
        val eventsDecorations: List<EventDecoration>
)

class TransactionAddress(val address: String?, val mine: Boolean)
