package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction

class NotSyncedTransaction(
        val hash: ByteArray,
        var transaction: RpcTransaction? = null,
        val timestamp: Long? = null
)
