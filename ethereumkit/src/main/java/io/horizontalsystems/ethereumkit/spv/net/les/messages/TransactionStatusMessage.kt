package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.spv.core.asString
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList

class TransactionStatusMessage(payload: ByteArray) : IInMessage {
    val requestID: Long
    val bv: Long
    val statuses: List<TransactionStatus>

    init {
        val statuses = mutableListOf<TransactionStatus>()
        val paramsList = RLP.decode2(payload)[0] as RLPList
        this.requestID = paramsList[0].rlpData.toLong()
        this.bv = paramsList[1].rlpData.toLong()
        val payloadList = paramsList[2] as RLPList
        for (i in 0 until payloadList.size) {
            val statusList = payloadList[i] as RLPList

            val status = when (statusList[0].rlpData.toInt()) {
                1 -> TransactionStatus.Queued
                2 -> TransactionStatus.Pending
                3 -> {
                    val dataList = statusList[1] as RLPList
                    TransactionStatus.Included(dataList[0].rlpData
                            ?: ByteArray(0), dataList[1].toLong(), dataList[2].toInt())
                }
                4 -> TransactionStatus.Error(statusList[1].asString())
                else -> TransactionStatus.Unknown
            }
            statuses.add(status)
        }

        this.statuses = statuses
    }

    override fun toString(): String {
        return "TransactionStatus [requestID: $requestID; bv: $bv; status: ${statuses[0].javaClass.name}]"
    }
}

sealed class TransactionStatus {
    object Unknown : TransactionStatus()
    object Queued : TransactionStatus()
    object Pending : TransactionStatus()
    class Included(val blockHash: ByteArray, val blockNumber: Long, val transactionIndex: Int) : TransactionStatus()
    class Error(val message: String) : TransactionStatus()

}
