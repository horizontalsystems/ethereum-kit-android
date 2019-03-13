package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class GetBlockHeadersMessage : IMessage {

    companion object {
        const val maxHeaders = 50
    }

    var requestID: Long = 0
    var blockHash: ByteArray = byteArrayOf()
    private var skip: Int = 0
    private var reverse: Int = 0

    constructor(requestID: Long,
                blockHash: ByteArray,
                skip: Int = 0,
                reverse: Int = 0) {
        this.requestID = requestID
        this.blockHash = blockHash
        this.skip = skip
        this.reverse = reverse
    }

    constructor(payload: ByteArray)

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