package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class TransactionSyncerState(
    @PrimaryKey
    val syncerId: String,
    val lastBlockNumber: Long
)
