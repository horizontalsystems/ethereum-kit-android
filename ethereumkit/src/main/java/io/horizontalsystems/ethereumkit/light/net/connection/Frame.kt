package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.light.net.messages.IMessage
import io.horizontalsystems.ethereumkit.light.net.messages.devp2p.DisconnectMessage
import io.horizontalsystems.ethereumkit.light.net.messages.devp2p.HelloMessage
import io.horizontalsystems.ethereumkit.light.net.messages.devp2p.PingMessage
import io.horizontalsystems.ethereumkit.light.net.messages.devp2p.PongMessage
import io.horizontalsystems.ethereumkit.light.net.messages.les.BlockHeadersMessage
import io.horizontalsystems.ethereumkit.light.net.messages.les.ProofsMessage
import io.horizontalsystems.ethereumkit.light.net.messages.les.StatusMessage

class Frame {
    var type: Int = 0
    var size: Int = 0
    var payload: ByteArray

    var totalFrameSize = -1
    var contextId = -1

    constructor(type: Int, payload: ByteArray) {
        this.type = type
        this.size = payload.size
        this.payload = payload
    }

    constructor(message: IMessage) {
        this.type = message.code
        this.payload = message.encoded()
        this.size = this.payload.size
    }

    override fun toString(): String {
        return "Frame [type: $type; size: $size; payload: ${payload.toHexString()}; " +
                "totalFrameSize: $totalFrameSize; contextId: $contextId]"
    }

    companion object {
        fun frameToMessage(frame: Frame): IMessage? =
                when (frame.type) {
                    HelloMessage.code -> HelloMessage(frame.payload)
                    DisconnectMessage.code -> DisconnectMessage(frame.payload)
                    PingMessage.code -> PingMessage()
                    PongMessage.code -> PongMessage()
                    StatusMessage.code -> StatusMessage(frame.payload)
                    BlockHeadersMessage.code -> BlockHeadersMessage(frame.payload)
                    ProofsMessage.code -> ProofsMessage(frame.payload)
                    else -> null
                }
    }
}