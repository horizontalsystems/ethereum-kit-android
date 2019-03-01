package io.horizontalsystems.ethereumkit.spv.net

import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

interface INetwork {
    val id: Int
    val genesisBlockHash: ByteArray
    val checkpointBlock: BlockHeader
}