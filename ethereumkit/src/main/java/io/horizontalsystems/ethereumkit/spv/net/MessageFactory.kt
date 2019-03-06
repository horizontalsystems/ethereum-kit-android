package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage

class MessageFactory {
    fun helloMessage(key: ECKey, capabilities: List<Capability>): HelloMessage {
        var nodeId = key.publicKeyPoint.getEncoded(false)
        nodeId = nodeId.copyOfRange(1, nodeId.size)
        return HelloMessage(nodeId, 30303, capabilities)
    }

    fun pongMessage(): PongMessage {
        return PongMessage()
    }
}
