package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.GasPriceJsonRpc

class LegacyGasPriceProvider(
        private val evmKit: EthereumKit
) {
    suspend fun gasPrice(): Long {
        return evmKit.rpc(GasPriceJsonRpc())
    }
}
