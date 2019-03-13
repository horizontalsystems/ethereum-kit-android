package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.GetBlockHeadersMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.GetProofsMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage
import java.util.*

class MessageFactory {
    fun helloMessage(key: ECKey, capabilities: List<Capability>): HelloMessage {
        var nodeId = key.publicKeyPoint.getEncoded(false)
        nodeId = nodeId.copyOfRange(1, nodeId.size)
        return HelloMessage(nodeId, 30303, capabilities)
    }

    fun pongMessage(): PongMessage {
        return PongMessage()
    }

    fun getBlockHeadersMessage(blockHash: ByteArray): GetBlockHeadersMessage {
        return GetBlockHeadersMessage(requestID = Math.abs(Random().nextLong()), blockHash = blockHash)
    }

    fun getProofsMessage(address: ByteArray, blockHash: ByteArray): GetProofsMessage {
        return GetProofsMessage(requestID = Math.abs(Random().nextLong()), blockHash = blockHash, key = address, key2 = ByteArray(0))
    }

    fun statusMessage(network: INetwork, blockHeader: BlockHeader): StatusMessage {
        return StatusMessage(
                protocolVersion = 2,
                networkId = network.id,
                genesisHash = network.genesisBlockHash,
                bestBlockTotalDifficulty = blockHeader.totalDifficulty,
                bestBlockHash = blockHeader.hashHex,
                bestBlockHeight = blockHeader.height)
    }

}
