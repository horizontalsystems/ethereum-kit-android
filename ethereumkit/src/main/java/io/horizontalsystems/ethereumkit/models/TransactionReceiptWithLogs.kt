package io.horizontalsystems.ethereumkit.models

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionReceiptWithLogs(
        @Embedded
        val receipt: TransactionReceipt,
        @Relation(parentColumn = "transactionHash", entityColumn = "transactionHash")
        val logs: List<TransactionLog>
)
