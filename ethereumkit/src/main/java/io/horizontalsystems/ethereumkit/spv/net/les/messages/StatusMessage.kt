package io.horizontalsystems.ethereumkit.spv.net.les.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.les.MaxCost
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import java.math.BigInteger

class StatusMessage : IInMessage, IOutMessage {

    val protocolVersion: Byte
    val networkId: Int
    val headTotalDifficulty: BigInteger
    val headHash: ByteArray
    val headHeight: Long
    val genesisHash: ByteArray

    var serveHeaders: Boolean = false
    var serveChainSince: BigInteger? = null
    var serveStateSince: BigInteger? = null

    var flowControlBL: Long = 0
    var flowControlMRR: Long = 0
    var flowControlMRC: List<MaxCost> = listOf()

    constructor(protocolVersion: Byte, networkId: Int,
                genesisHash: ByteArray, headTotalDifficulty: BigInteger,
                headHash: ByteArray, headHeight: Long) {
        this.protocolVersion = protocolVersion
        this.networkId = networkId
        this.headTotalDifficulty = headTotalDifficulty
        this.headHash = headHash
        this.headHeight = headHeight
        this.genesisHash = genesisHash
    }

    constructor(payload: ByteArray) {
        val params = RLP.decode2(payload)[0] as RLPList

        protocolVersion = params.valueElement("protocolVersion")?.rlpData?.get(0) ?: 0
        networkId = params.valueElement("networkId").toInt()
        headTotalDifficulty = params.valueElement("headTd").toBigInteger()
        headHash = params.valueElement("headHash")?.rlpData ?: byteArrayOf()
        headHeight = params.valueElement("headNum").toLong()
        genesisHash = params.valueElement("genesisHash")?.rlpData ?: byteArrayOf()

        serveHeaders = params.valueElement("serveHeaders") != null
        serveChainSince = params.valueElement("serveChainSince")?.toBigInteger()
        serveStateSince = params.valueElement("serveStateSince")?.toBigInteger()

        flowControlBL = params.valueElement("flowControl/BL").toLong()
        flowControlMRR = params.valueElement("flowControl/MRR").toLong()
        flowControlMRC = (params.valueElement("flowControl/MRC") as RLPList).map {
            MaxCost(it as RLPList)
        }
    }

    override fun encoded(): ByteArray {
        val protocolVersion = RLP.encodeList(RLP.encodeString("protocolVersion"), RLP.encodeByte(this.protocolVersion))
        val networkId = RLP.encodeList(RLP.encodeString("networkId"), RLP.encodeInt(this.networkId))
        val totalDifficulty = RLP.encodeList(RLP.encodeString("headTd"), RLP.encodeBigInteger(this.headTotalDifficulty))
        val bestHash = RLP.encodeList(RLP.encodeString("headHash"), RLP.encodeElement(this.headHash))
        val bestNum = RLP.encodeList(RLP.encodeString("headNum"), RLP.encodeLong(this.headHeight))
        val genesisHash = RLP.encodeList(RLP.encodeString("genesisHash"), RLP.encodeElement(this.genesisHash))
        val announceType = RLP.encodeList(RLP.encodeString("announceType"), RLP.encodeByte(1.toByte()))

        return RLP.encodeList(protocolVersion, networkId, totalDifficulty, bestHash, bestNum, genesisHash, announceType)
    }

    override fun toString(): String {
        return "Status [protocolVersion: $protocolVersion; networkId: $networkId; " +
                "totalDifficulty: $headTotalDifficulty; " +
                "bestHash: ${headHash.toHexString()}; bestNum: $headHeight; " +
                "genesisHash: ${genesisHash.toHexString()} " +
                "serveHeaders: $serveHeaders; " +
                "serveChainSince: $serveChainSince; " +
                "serveStateSince: $serveStateSince; " +
                "flowControlBL: ${String.format("%,d", flowControlBL)}; " +
                "flowControlMRR: ${String.format("%,d", flowControlMRR)}; " +
                "flowControlMRC: \n${flowControlMRC.joinToString(separator = ",\n ") { "$it" }}" +
                "]"
    }

}
