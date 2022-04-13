package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import java.lang.reflect.Type

class GetBlockByNumberJsonRpc(
        @Transient val blockNumber: Long
) : JsonRpc<RpcBlock>(
        method = "eth_getBlockByNumber",
        params = listOf(blockNumber, false)
) {
    @Transient
    override val typeOfResult: Type = object : TypeToken<RpcBlock>() {}.type
}
