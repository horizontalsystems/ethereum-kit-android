package io.horizontalsystems.ethereumkit.api.jsonrpc

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter

class GetTransactionCountJsonRpc(
        @Transient val address: Address,
        @Transient val defaultBlockParameter: DefaultBlockParameter
) : LongJsonRpc(
        method = "eth_getTransactionCount",
        params = listOf(address, defaultBlockParameter)
)
