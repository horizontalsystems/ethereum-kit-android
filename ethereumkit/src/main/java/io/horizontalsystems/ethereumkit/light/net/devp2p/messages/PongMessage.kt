package io.horizontalsystems.ethereumkit.light.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.light.net.IP2PMessage

class PongMessage : IP2PMessage {
    override var code = PongMessage.code

    override fun encoded(): ByteArray {
        return payload
    }

    override fun toString(): String {
        return "Pong"
    }

    companion object {
        const val code = 0x03
        val payload = "C0".hexStringToByteArray()
    }
}