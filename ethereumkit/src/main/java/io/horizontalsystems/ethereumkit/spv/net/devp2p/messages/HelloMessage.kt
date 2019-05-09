package io.horizontalsystems.ethereumkit.spv.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toInt
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import java.util.*

class HelloMessage : IInMessage, IOutMessage {
    var nodeId: ByteArray = byteArrayOf()
    var port: Int = 0
    var p2pVersion: Byte = 4
    var clientId: String = "EthereumKit"
    var capabilities: List<Capability> = listOf()

    constructor(nodeId: ByteArray, port: Int, capabilities: List<Capability>) {
        this.nodeId = nodeId
        this.port = port
        this.capabilities = capabilities
    }

    constructor(payload: ByteArray) {
        val paramsList = RLP.decode2(payload)[0] as RLPList

        val p2pVersionBytes = paramsList[0].rlpData
        p2pVersion = p2pVersionBytes?.get(0) ?: 0

        clientId = String(paramsList[1].rlpData ?: byteArrayOf())

        val capabilityList = paramsList[2] as RLPList
        val caps = ArrayList<Capability>()
        for (aCapabilityList in capabilityList) {

            val capId = (aCapabilityList as RLPList)[0]
            val capVersion = (aCapabilityList)[1]

            val name = String(capId.rlpData ?: byteArrayOf())

            val version = (capVersion.rlpData?.get(0) ?: 0).toByte()

            val cap = Capability(name, version)
            caps.add(cap)
        }

        capabilities = caps

        val peerPortBytes = paramsList[3].rlpData
        port = peerPortBytes.toInt()

        nodeId = paramsList[4].rlpData ?: byteArrayOf()
    }

    override fun encoded(): ByteArray {
        val p2pVersion = RLP.encodeByte(this.p2pVersion)
        val clientId = RLP.encodeString(this.clientId)
        val capabilities = arrayOfNulls<ByteArray>(this.capabilities.size)
        for (i in this.capabilities.indices) {
            val capability = this.capabilities[i]
            capabilities[i] = RLP.encodeList(
                    RLP.encodeElement(capability.name.toByteArray()),
                    RLP.encodeInt(capability.version.toInt()))
        }
        val capabilityList = RLP.encodeList(*capabilities.mapNotNull { it }.toTypedArray())

        val peerPort = RLP.encodeInt(this.port)
        val peerId = RLP.encodeElement(this.nodeId)

        return RLP.encodeList(p2pVersion, clientId, capabilityList, peerPort, peerId)
    }

    override fun toString(): String {
        return "Hello [version: $p2pVersion; clientId: $clientId; " +
                "capabilities: ${capabilities.joinToString { "${it.name}/${it.version}" }}; " +
                "nodeId: ${nodeId.toHexString()}; port: $port]"
    }
}
