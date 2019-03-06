package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.*
import java.util.*


class LESPeer(private val network: INetwork,
              private val bestBlock: BlockHeader,
              private val key: ECKey,
              private val node: Node,
              private val listener: Listener) : DevP2PPeer.Listener {

    interface Listener {
        fun connected()
        fun blocksReceived(blockHeaders: List<BlockHeader>)
        fun proofReceived(message: ProofsMessage)
    }

    companion object {
        val capability = Capability("les", 2,
                hashMapOf(0x00 to StatusMessage::class,
                        0x02 to GetBlockHeadersMessage::class,
                        0x03 to BlockHeadersMessage::class,
                        0x0f to GetProofsMessage::class,
                        0x10 to ProofsMessage::class))
    }

    private val protocolVersion: Byte = 2
    private val devP2PPeer: DevP2PPeer

    private var statusSent = false
    private var statusReceived = false

    init {
        devP2PPeer = DevP2PPeer.getInstance(node, key, capability)
        devP2PPeer.listener = this
    }

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
        try {
            validatePeer(message)
            proceedHandshake()
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    private fun validatePeer(message: StatusMessage) {
        check(message.networkId == network.id && message.genesisHash.contentEquals(network.genesisBlockHash)) {
            throw LESPeerError.WrongNetwork()
        }
        check(message.bestBlockHeight > 0.toBigInteger()) {
            throw LESPeerError.InvalidBestBlockHeight()
        }
        check(message.bestBlockHeight >= bestBlock.height) {
            throw LESPeerError.ExpiredBestBlockHeight()
        }
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

    //-----------DevP2PPeer.Listener methods------------

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

    open class LESPeerError : Exception() {
        class WrongNetwork : LESPeerError()
        class InvalidBestBlockHeight : LESPeerError()
        class ExpiredBestBlockHeight : LESPeerError()
    }
}