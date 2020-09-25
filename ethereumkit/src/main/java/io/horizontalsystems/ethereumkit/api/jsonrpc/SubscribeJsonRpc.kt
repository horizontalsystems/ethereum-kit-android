package io.horizontalsystems.ethereumkit.api.jsonrpc

class SubscribeJsonRpc(
        params: List<Any>
) : LongJsonRpc("eth_subscribe", params)
