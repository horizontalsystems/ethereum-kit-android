package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import java.math.BigInteger

@Entity
data class NotSyncTransactionRecord(
        @PrimaryKey
        val hash: ByteArray,
        val nonce: Long?,
        val blockHash: ByteArray?,
        val blockNumber: Long?,
        val transactionIndex: Int?,
        val from: Address?,
        val to: Address?,
        val value: BigInteger?,
        val gasPrice: Long?,
        val gasLimit: Long?,
        val input: ByteArray?,
        val timestamp: Long?

) {

    constructor(notSynced: NotSyncedTransaction) : this(
            notSynced.hash,
            notSynced.transaction?.nonce,
            notSynced.transaction?.blockHash,
            notSynced.transaction?.blockNumber,
            notSynced.transaction?.transactionIndex,
            notSynced.transaction?.from,
            notSynced.transaction?.to,
            notSynced.transaction?.value,
            notSynced.transaction?.gasPrice,
            notSynced.transaction?.gasLimit,
            notSynced.transaction?.input,
            notSynced.timestamp
    )

    fun asNotSyncedTransaction(): NotSyncedTransaction {
        val transactionDto = try {
            RpcTransaction(hash, nonce!!, blockHash, blockNumber, transactionIndex, from!!, to, value!!, gasPrice!!, gasLimit!!, input!!)
        } catch (error: Throwable) {
            null
        }
        return NotSyncedTransaction(hash, transactionDto, timestamp)
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NotSyncTransactionRecord)
            return false

        return hash.contentEquals(other.hash)
    }

    override fun hashCode(): Int {
        return hash.contentHashCode()
    }

}
