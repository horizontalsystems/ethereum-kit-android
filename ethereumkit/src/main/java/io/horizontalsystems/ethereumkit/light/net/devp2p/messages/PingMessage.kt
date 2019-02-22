package io.horizontalsystems.ethereumkit.light.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.light.net.IMessage

class PingMessage : IMessage {
    override var code = Companion.code

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