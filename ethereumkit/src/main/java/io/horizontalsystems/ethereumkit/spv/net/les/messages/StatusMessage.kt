package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import java.math.BigInteger

class StatusMessage : IMessage {

    val protocolVersion: Byte
    val networkId: Int
    val genesisHash: ByteArray
    val bestBlockTotalDifficulty: BigInteger
    val bestBlockHash: ByteArray
    val bestBlockHeight: BigInteger

    constructor(protocolVersion: Byte, networkId: Int,
                genesisHash: ByteArray, bestBlockTotalDifficulty: BigInteger,
                bestBlockHash: ByteArray, bestBlockHeight: BigInteger) {
        this.protocolVersion = protocolVersion
        this.networkId = networkId
        this.genesisHash = genesisHash
        this.bestBlockTotalDifficulty = bestBlockTotalDifficulty
        this.bestBlockHash = bestBlockHash
        this.bestBlockHeight = bestBlockHeight
    }

    constructor(payload: ByteArray) {
        val paramsList = RLP.decode2(payload)[0] as RLPList

        protocolVersion = (paramsList[0] as RLPList)[1].rlpData?.get(0) ?: 0
        val networkIdBytes = (paramsList[1] as RLPList)[1].rlpData

        networkId = networkIdBytes.toInt()

        bestBlockTotalDifficulty = (paramsList[2] as RLPList)[1].rlpData.toBigInteger()
        bestBlockHash = (paramsList[3] as RLPList)[1].rlpData ?: byteArrayOf()
        bestBlockHeight = (paramsList[4] as RLPList)[1].rlpData.toBigInteger()
        genesisHash = (paramsList[5] as RLPList)[1].rlpData ?: byteArrayOf()
    }

    override fun encoded(): ByteArray {
        val protocolVersion = RLP.encodeList(RLP.encodeString("protocolVersion"), RLP.encodeByte(this.protocolVersion))
        val networkId = RLP.encodeList(RLP.encodeString("networkId"), RLP.encodeInt(this.networkId))
        val totalDifficulty = RLP.encodeList(RLP.encodeString("headTd"), RLP.encodeBigInteger(this.bestBlockTotalDifficulty))
        val bestHash = RLP.encodeList(RLP.encodeString("headHash"), RLP.encodeElement(this.bestBlockHash))
        val bestNum = RLP.encodeList(RLP.encodeString("headNum"), RLP.encodeBigInteger(this.bestBlockHeight))
        val genesisHash = RLP.encodeList(RLP.encodeString("genesisHash"), RLP.encodeElement(this.genesisHash))
        val announceType = RLP.encodeList(RLP.encodeString("announceType"), RLP.encodeByte(1.toByte()))

        return RLP.encodeList(protocolVersion, networkId, totalDifficulty, bestHash, bestNum, genesisHash, announceType)
    }

    override fun toString(): String {
        return "Status [protocolVersion: $protocolVersion; networkId: $networkId; " +
                "totalDifficulty: $bestBlockTotalDifficulty; " +
                "bestHash: ${bestBlockHash.toHexString()}; bestNum: $bestBlockHeight; " +
                "genesisHash: ${genesisHash.toHexString()}]"
    }
}
