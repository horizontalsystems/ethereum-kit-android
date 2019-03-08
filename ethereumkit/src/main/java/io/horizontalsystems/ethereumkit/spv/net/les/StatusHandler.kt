package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage

class StatusHandler(val network: INetwork, val blockHeader: BlockHeader) {

    @Throws(Exception::class)
    fun validate(message: StatusMessage) {
        check(message.networkId == network.id && message.genesisHash.contentEquals(network.genesisBlockHash)) {
            throw LESPeer.LESPeerError.WrongNetwork()
        }
        check(message.bestBlockHeight > 0.toBigInteger()) {
            throw LESPeer.LESPeerError.InvalidBestBlockHeight()
        }
        check(message.bestBlockHeight >= blockHeader.height) {
            throw LESPeer.LESPeerError.ExpiredBestBlockHeight()
        }
    }
}
