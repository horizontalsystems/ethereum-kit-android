package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity
class Transaction(
        @PrimaryKey
        val hash: ByteArray,
        val nonce: Long,
        val from: Address,
        val to: Address?,
        val value: BigInteger,
        val gasPrice: Long,
        val gasLimit: Long,
        val input: ByteArray,
        val timestamp: Long
)
