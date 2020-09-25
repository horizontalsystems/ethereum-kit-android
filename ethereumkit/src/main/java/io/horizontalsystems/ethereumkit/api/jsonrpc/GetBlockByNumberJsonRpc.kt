package io.horizontalsystems.ethereumkit.api.jsonrpc

import io.horizontalsystems.ethereumkit.models.Block

class GetBlockByNumberJsonRpc(
        @Transient val blockNumber: Long
) : JsonRpc<Block>(
        method = "eth_getBlockByNumber",
        params = listOf(blockNumber, false)
) {
    @Transient
    override val typeOfResult = Block::class.java
}
