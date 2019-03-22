package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.helpers.RandomHelper
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.*
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.*

class LESPeer(private val devP2PPeer: DevP2PPeer,
              private val network: INetwork,
              private val lastBlockHeader: BlockHeader,
              private val randomHelper: RandomHelper,
              private val requestHolder: LESPeerRequestHolder = LESPeerRequestHolder()) : IPeer, DevP2PPeer.Listener {

    override var listener: IPeerListener? = null

    override val syncState: EthereumKit.SyncState
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    private fun handle(message: IMessage) {
        when (message) {
            is StatusMessage -> handle(message)
            is BlockHeadersMessage -> handle(message)
            is ProofsMessage -> handle(message)
            is AnnounceMessage -> handle(message)
        }
    }

    private fun handle(message: StatusMessage) {
        check(message.protocolVersion == LESPeer.capability.version) {
            throw InvalidProtocolVersion()
        }

        check(message.networkId == network.id) {
            throw WrongNetwork()
        }

        check(message.genesisHash.contentEquals(network.genesisBlockHash)) {
            throw WrongNetwork()
        }

        check(message.headHeight >= lastBlockHeader.height) {
            throw ExpiredBestBlockHeight()
        }

        listener?.didConnect()
    }

    private fun handle(message: BlockHeadersMessage) {
        val request = requestHolder.removeBlockHeaderRequest(message.requestID)

        checkNotNull(request) {
            throw UnexpectedMessage()
        }

        listener?.didReceive(message.headers, request.blockHeader, request.reversed)
    }

    private fun handle(message: ProofsMessage) {
        val request = requestHolder.removeAccountStateRequest(message.requestID)

        checkNotNull(request) {
            throw UnexpectedMessage()
        }

        listener?.didReceive(request.getAccountState(message), request.address, request.blockHeader)
    }

    private fun handle(message: AnnounceMessage) {
        listener?.didAnnounce(message.blockHash, message.blockHeight)
    }

    //------------------Public methods----------------------

    override fun connect() {
        devP2PPeer.connect()
    }

    override fun disconnect(error: Throwable?) {
        devP2PPeer.disconnect(error)
    }

    override fun requestBlockHeaders(blockHeader: BlockHeader, limit: Int, reversed: Boolean) {
        val requestId = randomHelper.randomLong()
        requestHolder.setBlockHeaderRequest(BlockHeaderRequest(blockHeader, reversed), requestId)

        val message = GetBlockHeadersMessage(requestId, blockHeader.height, limit)
        devP2PPeer.send(message)
    }

    override fun requestAccountState(address: ByteArray, blockHeader: BlockHeader) {
        val requestId = randomHelper.randomLong()
        requestHolder.setAccountStateRequest(AccountStateRequest(address, blockHeader), requestId)

        val message = GetProofsMessage(requestId, blockHeader.hashHex, address)
        devP2PPeer.send(message)
    }

    //-----------DevP2PPeer.Listener methods------------

    override fun didConnect() {
        println("LESPeer -> didConnect\n")
        val statusMessage = StatusMessage(
                protocolVersion = capability.version,
                networkId = network.id,
                genesisHash = network.genesisBlockHash,
                headTotalDifficulty = lastBlockHeader.totalDifficulty,
                headHash = lastBlockHeader.hashHex,
                headHeight = lastBlockHeader.height)

        devP2PPeer.send(statusMessage)
    }

    override fun didDisconnect(error: Throwable?) {
        println("LESPeer -> didDisconnect: $error")
        listener?.didDisconnect(error)
    }

    override fun didReceive(message: IMessage) {
        try {
            handle(message)
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    companion object {
        val capability = Capability("les", 2,
                hashMapOf(0x00 to StatusMessage::class,
                        0x01 to AnnounceMessage::class,
                        0x02 to GetBlockHeadersMessage::class,
                        0x03 to BlockHeadersMessage::class,
                        0x0f to GetProofsMessage::class,
                        0x10 to ProofsMessage::class))

        fun getInstance(network: INetwork, bestBlock: BlockHeader, key: ECKey, node: Node): LESPeer {
            val devP2PPeer = DevP2PPeer.getInstance(key, node, listOf(capability))
            val lesPeer = LESPeer(devP2PPeer, network, bestBlock, RandomHelper)
            devP2PPeer.listener = lesPeer

            return lesPeer
        }

    }

    open class LESPeerError : Exception()

    open class LESPeerConsistencyError : Exception()
    class UnexpectedMessage : LESPeerConsistencyError()

    open class LESPeerValidationError : LESPeerError()
    class InvalidProtocolVersion : LESPeerValidationError()
    class WrongNetwork : LESPeerValidationError()
    class ExpiredBestBlockHeight : LESPeerValidationError()

}
