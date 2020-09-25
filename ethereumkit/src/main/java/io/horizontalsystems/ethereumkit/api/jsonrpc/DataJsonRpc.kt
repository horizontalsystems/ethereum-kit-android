package io.horizontalsystems.ethereumkit.api.jsonrpc

open class DataJsonRpc(
        method: String,
        params: List<Any>
) : JsonRpc<ByteArray>(method, params) {
    @Transient
    override val typeOfResult = ByteArray::class.java
}
