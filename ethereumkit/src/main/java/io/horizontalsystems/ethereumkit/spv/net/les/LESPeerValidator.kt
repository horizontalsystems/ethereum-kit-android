package io.horizontalsystems.ethereumkit.spv.net.les

import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage

class LESPeerValidator {

    @Throws(Exception::class)
    fun validate(message: StatusMessage, network: INetwork, blockHeader: BlockHeader) {
        check(message.networkId == network.id && message.genesisHash.contentEquals(network.genesisBlockHash)) {
            throw LESPeer.WrongNetwork()
        }
        check(message.bestBlockHeight >= blockHeader.height) {
            throw LESPeer.ExpiredBestBlockHeight()
        }
    }
}
