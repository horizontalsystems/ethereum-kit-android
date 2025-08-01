package io.horizontalsystems.merkleiokit

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MerkleTransactionHash(
    @PrimaryKey
    val hash: ByteArray
)
