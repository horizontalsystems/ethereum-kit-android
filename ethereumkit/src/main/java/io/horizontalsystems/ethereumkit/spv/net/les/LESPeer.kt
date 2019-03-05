package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.devp2p.IDevP2PPeerListener
import io.horizontalsystems.ethereumkit.spv.net.les.messages.*
import java.util.*


interface IPeerListener {
    fun connected()
    fun blocksReceived(blockHeaders: List<BlockHeader>)
    fun proofReceived(message: ProofsMessage)
}

class Peer(val network: INetwork, val bestBlock: BlockHeader, key: ECKey, val node: Node, val listener: IPeerListener) : IDevP2PPeerListener {

    companion object {
        val capability = Capability("les", 2,
                hashMapOf(0x00 to StatusMessage::class,
                        0x02 to GetBlockHeadersMessage::class,
                        0x03 to BlockHeadersMessage::class,
                        0x0f to GetProofsMessage::class,
                        0x10 to ProofsMessage::class))
    }

    private val protocolVersion: Byte = 2
    private var devP2PPeer: DevP2PPeer = DevP2PPeer(key, node, Peer.capability, this)

    var statusSent = false
    var statusReceived = false

    private fun proceedHandshake() {
        if (statusSent) {
            if (statusReceived) {
                listener.connected()
                return
            }
        } else {
            val statusMessage = StatusMessage(
                    protocolVersion = protocolVersion,
                    networkId = network.id,
                    genesisHash = network.genesisBlockHash,
                    bestBlockTotalDifficulty = bestBlock.totalDifficulty,
                    bestBlockHash = bestBlock.hashHex,
                    bestBlockHeight = bestBlock.height
            )

            devP2PPeer.send(statusMessage)
            statusSent = true
        }
    }

    private fun handle(message: IMessage) {
        when (message) {
            is StatusMessage -> handle(message)
            is BlockHeadersMessage -> handle(message)
            is ProofsMessage -> handle(message)
        }
    }

    private fun handle(message: StatusMessage) {
        statusReceived = true

        proceedHandshake()
    }

    private fun handle(message: BlockHeadersMessage) {
        listener.blocksReceived(message.headers.drop(1))
    }

    private fun handle(message: ProofsMessage) {
        listener.proofReceived(message)
    }

    //------------------Public methods----------------------

    fun connect() {
        devP2PPeer.connect()
    }

    fun disconnect(error: Throwable?) {
        devP2PPeer.disconnect(error)
    }

    fun downloadBlocksFrom(block: BlockHeader) {
        val message = GetBlockHeadersMessage(requestID = Math.abs(Random().nextLong()), blockHash = block.hashHex)

        devP2PPeer.send(message)
    }

    fun getBalance(address: ByteArray, blockHash: ByteArray) {
        val message = GetProofsMessage(requestID = Math.abs(Random().nextLong()), blockHash = blockHash, key = address, key2 = ByteArray(0))

        devP2PPeer.send(message)
    }

    //-----------IDevP2PPeerListener methods------------

    override fun onConnectionEstablished() {
        println("Peer -> onConnectionEstablished\n")
        proceedHandshake()
    }

    override fun onDisconnected(error: Throwable?) {
        println("Peer -> onDisconnected")
    }

    override fun onMessageReceived(message: IMessage) {
        println("Peer -> onMessageReceived\n")
        handle(message)
    }
}