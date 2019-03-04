package io.horizontalsystems.ethereumkit.spv.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.net.IP2PMessage

class PingMessage : IP2PMessage {
    override var code = PingMessage.code

    override fun encoded(): ByteArray {
        return payload
    }

    override fun toString(): String {
        return "Ping"
    }

    companion object {
        const val code = 0x02
        val payload = "C0".hexStringToByteArray()
    }
}