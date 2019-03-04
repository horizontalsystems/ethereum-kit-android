package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.net.ILESMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class GetBlockHeadersMessage(val requestID: Long,
                             val blockHash: ByteArray,
                             val skip: Int = 0,
                             val reverse: Int = 0) : ILESMessage {
    companion object {
        const val code = 0x02
        const val maxHeaders = 50
    }

    override var code: Int = Companion.code

    override fun encoded(): ByteArray {
        val reqID = RLP.encodeBigInteger(BigInteger.valueOf(this.requestID))
        val maxHeaders = RLP.encodeInt(maxHeaders)
        val skipBlocks = RLP.encodeInt(skip)
        val reverse = RLP.encodeByte(reverse.toByte())
        val hash = RLP.encodeElement(this.blockHash)

        var encoded = RLP.encodeList(hash, maxHeaders, skipBlocks, reverse)
        encoded = RLP.encodeList(reqID, encoded)

        return encoded
    }

    override fun toString(): String {
        return "GetHeaders [requestId: $requestID; blockHash: ${blockHash.toHexString()}; maxHeaders: $maxHeaders; skip: $skip; reverse: $reverse]"
    }
}