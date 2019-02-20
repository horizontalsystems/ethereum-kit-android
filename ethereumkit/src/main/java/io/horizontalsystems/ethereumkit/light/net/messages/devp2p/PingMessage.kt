package io.horizontalsystems.ethereumkit.light.net.messages.devp2p

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.light.net.messages.IMessage

class PingMessage : IMessage {
    override var code = PingMessage.code

    override fun encoded(): ByteArray {
        return PingMessage.payload
    }

    override fun toString(): String {
        return "Ping"
    }

    companion object {
        const val code = 0x02
        val payload = "C0".hexStringToByteArray()
    }
}