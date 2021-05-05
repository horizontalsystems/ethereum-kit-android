package io.horizontalsystems.ethereumkit.network

import io.horizontalsystems.ethereumkit.spv.models.BlockHeader

interface INetwork {
    val id: Int
    val genesisBlockHash: ByteArray
    val checkpointBlock: BlockHeader
    val blockTime: Long
}