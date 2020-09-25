package io.horizontalsystems.ethereumkit.api.jsonrpc

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter

class GetStorageAtJsonRpc(
        @Transient val contractAddress: Address,
        @Transient val position: ByteArray,
        @Transient val defaultBlockParameter: DefaultBlockParameter
) : DataJsonRpc(
        method = "eth_getStorageAt",
        params = listOf(contractAddress, position, defaultBlockParameter)
)
