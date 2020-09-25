package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.models.RpcTransaction
import java.lang.reflect.Type
import java.util.*

class GetTransactionByHashJsonRpc(
        @Transient val transactionHash: ByteArray
) : JsonRpc<Optional<RpcTransaction>>(
        method = "eth_getTransactionByHash",
        params = listOf(transactionHash)
) {
    @Transient
    override val typeOfResult: Type = object : TypeToken<Optional<RpcTransaction>>() {}.type
}
