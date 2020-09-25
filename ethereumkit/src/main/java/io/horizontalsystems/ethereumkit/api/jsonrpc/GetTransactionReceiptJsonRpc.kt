package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.reflect.TypeToken
import io.horizontalsystems.ethereumkit.models.TransactionReceipt
import java.lang.reflect.Type
import java.util.*

class GetTransactionReceiptJsonRpc(
        @Transient val transactionHash: ByteArray
) : JsonRpc<Optional<TransactionReceipt>>(
        method = "eth_getTransactionReceipt",
        params = listOf(transactionHash)
) {
    @Transient
    override val typeOfResult: Type = object : TypeToken<Optional<TransactionReceipt>>() {}.type
}
