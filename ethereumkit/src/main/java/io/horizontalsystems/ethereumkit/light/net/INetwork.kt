package io.horizontalsystems.ethereumkit.light.net

import io.horizontalsystems.ethereumkit.light.models.BlockHeader

interface INetwork {
    val id: Int
    val genesisBlockHash: ByteArray
    val checkpointBlock: BlockHeader
}