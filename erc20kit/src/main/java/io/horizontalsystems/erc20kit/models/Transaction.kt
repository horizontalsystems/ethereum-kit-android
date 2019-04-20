package io.horizontalsystems.erc20kit.models

import android.arch.persistence.room.Entity
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import java.math.BigInteger
import java.util.*

@Entity(primaryKeys = ["transactionHash", "logIndex"])
class Transaction(var transactionHash: ByteArray,
                  val contractAddress: ByteArray,
                  val from: ByteArray,
                  val to: ByteArray,
                  val value: BigInteger,
                  var timestamp: Long = System.currentTimeMillis() / 1000) {


    companion object {
        val transferEventTopic = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef".hexStringToByteArray()

        fun createFromLog(log: EthereumLog): Transaction? {
            if (log.topics.count() != 3 || log.topics[0] != transferEventTopic.toHexString() ||
                    log.topics[1].hexStringToByteArray().count() != 32 || log.topics[2].hexStringToByteArray().count() != 32)
                return null

            return Transaction(transactionHash = log.transactionHash.hexStringToByteArray(),
                    contractAddress = log.address.hexStringToByteArray(),
                    from = log.topics[1].hexStringToByteArray().copyOfRange(12, 32),
                    to = log.topics[2].hexStringToByteArray().copyOfRange(12, 32),
                    value = log.data.hexStringToByteArray().toBigInteger(),
                    timestamp = log.timestamp ?: System.currentTimeMillis() / 1000).apply {
                logIndex = log.logIndex
                blockHash = log.blockHash.hexStringToByteArray()
                blockNumber = log.blockNumber
            }
        }
    }

    var logIndex: Int = 0
    var blockHash: ByteArray? = null
    var blockNumber: Long? = null

    override fun equals(other: Any?): Boolean {
        if (other !is Transaction)
            return false

        return transactionHash.contentEquals(other.transactionHash) && logIndex == other.logIndex
    }

    override fun hashCode(): Int {
        return Objects.hash(transactionHash, logIndex)
    }


}
