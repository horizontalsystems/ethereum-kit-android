package io.horizontalsystems.ethereumkit.api

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.reactivex.Single
import java.math.BigInteger

typealias RpcHandler = (RpcResponse) -> Unit
typealias SubscriptionHandler = (RpcSubscriptionResponse) -> Unit

data class RpcResponse(val id: Int, val result: JsonElement?, val error: Error?) {
    data class Error(val code: Int, val message: String)
}
data class RpcSubscriptionResponse(val method: String, val params: Params) {
    data class Params(@SerializedName("subscription") val subscriptionId: Long, val result: JsonElement)
}
data class RpcGeneralResponse(val id: Int?, val result: JsonElement?, val error: RpcResponse.Error?, val method: String?, val params: RpcSubscriptionResponse.Params?)

interface IRpcWebSocket {
    var listener: IRpcWebSocketListener?
    val source: String

    fun start()
    fun stop()
    fun <T> send(rpc: JsonRpc<T>)
}

interface IRpcWebSocketListener {
    fun didUpdate(state: WebSocketState)
    fun didReceive(response: RpcResponse)
    fun didReceive(response: RpcSubscriptionResponse)
}

sealed class WebSocketState {
    object Connecting : WebSocketState()
    object Connected : WebSocketState()
    class Disconnected(val error: Throwable) : WebSocketState()

    sealed class DisconnectError : Throwable() {
        object NotStarted : DisconnectError()
        class SocketDisconnected(val reason: String) : DisconnectError()
    }
}

interface IRpcSyncer {
    var listener: IRpcSyncerListener?

    val source: String
    val syncState: SyncState

    fun start()
    fun stop()
    fun refresh()
    fun <T> single(rpc: JsonRpc<T>): Single<T>
}

interface IRpcSyncerListener {
    fun didUpdateSyncState(syncState: SyncState)
    fun didUpdateLastBlockLogsBloom(lastBlockLogsBloom: String)
    fun didUpdateLastBlockHeight(lastBlockHeight: Long)
    fun didUpdateBalance(balance: BigInteger)
}
