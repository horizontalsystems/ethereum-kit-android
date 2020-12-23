package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import java.lang.reflect.Type
import java.util.*

class GetBlockByNumberJsonRpc(
        @Transient val blockNumber: Long
) : JsonRpc<Optional<RpcBlock>>(
        method = "eth_getBlockByNumber",
        params = listOf(blockNumber, false)
) {
    @Transient
    override val typeOfResult: Type = object : TypeToken<Optional<RpcBlock>>() {}.type
}
