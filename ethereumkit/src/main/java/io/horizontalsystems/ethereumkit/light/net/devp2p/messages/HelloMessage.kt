package io.horizontalsystems.ethereumkit.light.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.net.IP2PMessage
import io.horizontalsystems.ethereumkit.light.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.rlp.RLPList
import io.horizontalsystems.ethereumkit.light.toInt
import java.util.*

class HelloMessage : IP2PMessage {

    companion object {
        const val code = 0x00
    }

    private var peerId: ByteArray = byteArrayOf()
    private var port: Int = 0
    private var p2pVersion: Byte = 4
    private var clientId: String = "EthereumKit"
    private var capabilities: List<Capability>

    constructor(peerId: ByteArray, port: Int, capabilities: List<Capability>) {
        this.peerId = peerId
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

        peerId = paramsList[4].rlpData ?: byteArrayOf()
    }

    override var code: Int = HelloMessage.code

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
        val peerId = RLP.encodeElement(this.peerId)

        return RLP.encodeList(p2pVersion, clientId, capabilityList, peerPort, peerId)
    }

    override fun toString(): String {
        return "Hello [version: $p2pVersion; clientId: $clientId; " +
                "capabilities: ${capabilities.joinToString { "${it.name}/${it.version}" }}; " +
                "peerId: ${peerId.toHexString()}; port: $port]"
    }
}