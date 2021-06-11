package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity

@Entity(primaryKeys = ["name", "hash"])
class TransactionTag(
        val name: String,
        val hash: ByteArray
)
