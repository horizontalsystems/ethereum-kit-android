package io.horizontalsystems.erc20kit.models

import android.arch.persistence.room.Entity
import java.math.BigInteger
import java.util.*

@Entity(primaryKeys = ["transactionHash", "interTransactionIndex"])
class Transaction(var transactionHash: ByteArray,
                  var interTransactionIndex: Int = 0,
                  var transactionIndex: Int? = null,
                  val from: ByteArray,
                  val to: ByteArray,
                  val value: BigInteger,
                  var timestamp: Long = System.currentTimeMillis() / 1000,
                  var isError: Boolean = false) {

    var logIndex: Int? = null
    var blockHash: ByteArray? = null
    var blockNumber: Long? = null

    override fun equals(other: Any?): Boolean {
        if (other !is Transaction)
            return false

        return transactionHash.contentEquals(other.transactionHash) && interTransactionIndex == other.interTransactionIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(transactionHash, interTransactionIndex)
    }

}
