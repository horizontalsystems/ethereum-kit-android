package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.core.ISpvStorage
import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

class BlockHelper(val storage: ISpvStorage, val network: INetwork) {

    val lastBlockHeader: BlockHeader
        get() = storage.getLastBlockHeader() ?: network.checkpointBlock

}
