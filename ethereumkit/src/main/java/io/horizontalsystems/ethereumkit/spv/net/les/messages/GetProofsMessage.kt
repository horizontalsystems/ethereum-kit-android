package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class GetProofsMessage : IOutMessage {

    var requestID: Long = 0
    var proofRequests: List<ProofRequest> = listOf()

    constructor(requestID: Long, blockHash: ByteArray, key: ByteArray, key2: ByteArray = byteArrayOf(), fromLevel: Int = 0) {
        this.requestID = requestID
        this.proofRequests = listOf(ProofRequest(blockHash, key, key2, fromLevel))
    }

    constructor(payload: ByteArray)

    override fun encoded(): ByteArray {
        val reqID = RLP.encodeBigInteger(BigInteger.valueOf(this.requestID))

        val encodedProofs = proofRequests.map { it.asRLPEncoded() }
        val proofsList = RLP.encodeList(*encodedProofs.toTypedArray())

        return RLP.encodeList(reqID, proofsList)
    }

    override fun toString(): String {
        return "GetProofs [requestID: $requestID; proofRequests: [${proofRequests.joinToString(separator = ",") { it.toString() }}]]"
    }

    class ProofRequest(val blockHash: ByteArray, val key: ByteArray, val key2: ByteArray, val fromLevel: Int) {

        private val keyHash: ByteArray = CryptoUtils.sha3(key)
        private val key2Hash: ByteArray

        init {
            if (key2.isNotEmpty()) {
                this.key2Hash = CryptoUtils.sha3(key2)
            } else {
                this.key2Hash = key2
            }
        }

        fun asRLPEncoded(): ByteArray {
            return RLP.encodeList(
                    RLP.encodeElement(blockHash),
                    RLP.encodeElement(key2Hash),
                    RLP.encodeElement(keyHash),
                    RLP.encodeInt(fromLevel)
            )
        }

        override fun toString(): String {
            return "(blockHash: ${blockHash.toHexString()}; key: ${keyHash.toHexString()}; key2: ${key2Hash.toHexString()}; fromLevel: $fromLevel)"
        }
    }
}
