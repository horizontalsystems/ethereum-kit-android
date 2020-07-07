package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigInteger

@Entity
class EthereumTransaction(
        @PrimaryKey val hash: ByteArray,
        val nonce: Long,
        val input: ByteArray,
        val from: ByteArray,
        val to: ByteArray,
        val value: BigInteger,
        val gasLimit: Long,
        val gasPrice: Long,
        val timestamp: Long,
        var blockHash: ByteArray? = null,
        var blockNumber: Long? = null,
        var gasUsed: Long? = null,
        var cumulativeGasUsed: Long? = null,
        var iserror: Int? = null,
        var transactionIndex: Int? = null,
        var txReceiptStatus: Int? = null
)
