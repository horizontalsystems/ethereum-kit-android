package io.horizontalsystems.ethereumkit.spv.net.devp2p.messages

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.net.IInMessage

class PingMessage() : IInMessage {

    constructor(payload: ByteArray) : this()

    override fun toString(): String {
        return "Ping"
    }

    companion object {
        val payload = "C0".hexStringToByteArray()
    }
}
