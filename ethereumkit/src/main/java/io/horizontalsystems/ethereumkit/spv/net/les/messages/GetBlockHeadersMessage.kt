package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class GetBlockHeadersMessage(var requestID: Long,
                             var blockHeight: Long,
                             var maxHeaders: Int,
                             private var skip: Int = 0,
                             private var reverse: Int = 0) : IOutMessage {

    override fun encoded(): ByteArray {
        val reqID = RLP.encodeBigInteger(BigInteger.valueOf(this.requestID))
        val maxHeaders = RLP.encodeInt(maxHeaders)
        val skipBlocks = RLP.encodeInt(skip)
        val reverse = RLP.encodeByte(reverse.toByte())
        val height = RLP.encodeLong(this.blockHeight)

        var encoded = RLP.encodeList(height, maxHeaders, skipBlocks, reverse)
        encoded = RLP.encodeList(reqID, encoded)

        return encoded
    }

    override fun toString(): String {
        return "GetHeaders [requestId: $requestID; blockHeight: ${blockHeight}; maxHeaders: $maxHeaders; skip: $skip; reverse: $reverse]"
    }
}
