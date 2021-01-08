package io.horizontalsystems.ethereumkit.api.jsonrpcsubscription

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.core.RpcSubscriptionResponse

abstract class RpcSubscription<T>(val params: List<Any>) {
    protected abstract val typeOfResult: Class<T>

    fun parse(response: RpcSubscriptionResponse, gson: Gson): T {
        return gson.fromJson(response.params.result.toString(), typeOfResult)
    }
}
