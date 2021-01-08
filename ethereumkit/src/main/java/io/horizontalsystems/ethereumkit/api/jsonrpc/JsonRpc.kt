package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.Gson
import com.google.gson.JsonElement
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

    fun parseResponse(response: RpcResponse, gson: Gson): T {
        if (response.error != null) {
            if (ResponseError.InsufficientBalance.causes.firstOrNull { response.error.message.contains(it) } != null) {
                throw ResponseError.InsufficientBalance(response.error)
            }
            throw ResponseError.RpcError(response.error)
        }
        return parseResult(response.result, gson)
    }

    fun parseResult(result: JsonElement?, gson: Gson): T {
        return try {
            gson.fromJson(result, typeOfResult) as T
        } catch (error: Throwable) {
            throw ResponseError.InvalidResult(result.toString())
        }
    }

    sealed class ResponseError : Throwable() {
        class RpcError(val error: RpcResponse.Error) : ResponseError()
        class InvalidResult(val result: Any?) : ResponseError()
        class InsufficientBalance(val result: Any?) : ResponseError() {
            companion object {
                val causes = listOf("execution reverted", "gas required exceeds", "insufficient funds for transfer")
            }
        }
    }
}
