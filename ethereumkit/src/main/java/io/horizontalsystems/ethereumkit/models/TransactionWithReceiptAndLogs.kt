package io.horizontalsystems.ethereumkit.models

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithReceiptAndLogs(
        @Embedded
        val transaction: Transaction,
        @Relation(
                entity = TransactionReceipt::class,
                parentColumn = "hash",
                entityColumn = "transactionHash"
        )
        val receipt: TransactionReceiptWithLogs
)