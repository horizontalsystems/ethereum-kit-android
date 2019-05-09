package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.spv.net.les.LESPeer

class PeerGroupState {
    var syncPeer: LESPeer? = null
    var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
}
