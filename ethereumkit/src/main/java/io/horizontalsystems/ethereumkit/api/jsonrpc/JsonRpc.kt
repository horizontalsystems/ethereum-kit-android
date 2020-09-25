package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.api.RpcResponse
import java.lang.reflect.Type

abstract class JsonRpc<T>(
        val method: String,
        val params: List<Any>
) {
    @SerializedName("jsonrpc")
    val version: String = "2.0"
    var id: Int = 1

    protected abstract val typeOfResult: Type

    fun parse(response: RpcResponse, gson: Gson): T {
        if (response.error != null) {
            throw ResponseError.RpcError(response.error)
        }
        return try {
            gson.fromJson(response.result, typeOfResult) as T
        } catch (error: Throwable) {
            throw ResponseError.InvalidResult(response.result.toString())
        }
    }

    sealed class ResponseError : Throwable() {
        class RpcError(val error: RpcResponse.Error) : ResponseError()
        class InvalidResult(val result: Any?) : ResponseError()
    }
}
