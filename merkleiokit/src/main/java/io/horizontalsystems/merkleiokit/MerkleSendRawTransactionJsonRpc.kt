package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.api.jsonrpc.DataJsonRpc

class MerkleSendRawTransactionJsonRpc(
    @Transient val signedTransaction: ByteArray,
    @Transient val sourceTag: String
) : DataJsonRpc(
    method = "eth_sendRawTransaction",
    params = listOf(signedTransaction, sourceTag)
)
