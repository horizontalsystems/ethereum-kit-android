package io.horizontalsystems.ethereumkit.models

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithInternal(
        @Embedded
        val transaction: EthereumTransaction,
        @Relation(parentColumn = "hash", entityColumn = "hash")
        val internalTransactions: List<InternalTransaction> = listOf()
)
