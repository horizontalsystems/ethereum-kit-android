package io.horizontalsystems.erc20kit.models

import androidx.room.Entity

@Entity(primaryKeys = ["primaryKey"])
data class TransactionSyncOrder(
        val value: Long,
        val primaryKey: String = "primaryKey"
)
