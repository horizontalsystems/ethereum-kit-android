package io.horizontalsystems.ethereumkit.models

import androidx.room.Entity

@Entity(primaryKeys = ["name", "hash"])
class TransactionTag(
        val name: String,
        val hash: ByteArray
) {

    companion object {
        const val EVM_COIN = "ETH"
        const val INCOMING = "incoming"
        const val OUTGOING = "outgoing"
        const val SWAP = "swap"
        const val EVM_COIN_INCOMING = "${EVM_COIN}_${INCOMING}"
        const val EVM_COIN_OUTGOING = "${EVM_COIN}_${OUTGOING}"
        const val EIP20_TRANSFER = "eip20Transfer"
        const val EIP20_APPROVE = "eip20Approve"

        fun tokenIncoming(contractAddress: String): String = "${contractAddress}_$INCOMING"
        fun tokenOutgoing(contractAddress: String): String = "${contractAddress}_$OUTGOING"

        fun fromAddress(address: String): String = "from_$address"
        fun toAddress(address: String): String = "to_$address"
    }

}
