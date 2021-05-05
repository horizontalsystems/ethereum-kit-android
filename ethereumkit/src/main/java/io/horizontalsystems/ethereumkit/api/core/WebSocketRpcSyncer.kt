package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.SubscribeJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpcsubscription.NewHeadsRpcSubscription
import io.horizontalsystems.ethereumkit.api.jsonrpcsubscription.RpcSubscription
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class WebSocketRpcSyncer(
        private val rpcSocket: IRpcWebSocket,
        private val gson: Gson
) : IRpcSyncer, IRpcWebSocketListener {
    private val logger = Logger.getLogger("WebSocketRpcSyncer")

    private var currentRpcId = AtomicInteger(0)
    private var rpcHandlers = ConcurrentHashMap<Int, RpcHandler>()
    private var subscriptionHandlers = ConcurrentHashMap<String, SubscriptionHandler>()

    //region IRpcSyncer
    override var listener: IRpcSyncerListener? = null

    override val source = "WebSocket ${rpcSocket.source}"

    override var state: SyncerState = SyncerState.NotReady(EthereumKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.didUpdateSyncerState(value)
            }
        }

    override fun start() {
        state = SyncerState.Preparing

        rpcSocket.start()
    }

    override fun stop() {
        state = SyncerState.NotReady(EthereumKit.SyncError.NotStarted())

        rpcSocket.stop()
    }

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        return Single.create { emitter ->
            send(
                    rpc = rpc,
                    onSuccess = {
                        emitter.onSuccess(it)
                    },
                    onError = {
                        emitter.onError(it)
                    }
            )
        }
    }
    //endregion

    //region IRpcWebSocketListener
    override fun didUpdate(socketState: WebSocketState) {
        when (socketState) {
            WebSocketState.Connecting -> {
                state = SyncerState.Preparing
            }
            WebSocketState.Connected -> {
                state = SyncerState.Ready
                subscribeToNewHeads()
            }
            is WebSocketState.Disconnected -> {
                rpcHandlers.forEach { (_, rpcHandler) ->
                    rpcHandler.onError(socketState.error)
                }
                rpcHandlers.clear()
                subscriptionHandlers.clear()

                state = SyncerState.NotReady(socketState.error)
            }
        }
    }

    override fun didReceive(response: RpcResponse) {
        rpcHandlers.remove(response.id)?.let { rpcHandler ->
            rpcHandler.onSuccess(response)
        }
    }

    override fun didReceive(response: RpcSubscriptionResponse) {
        subscriptionHandlers[response.params.subscriptionId]?.invoke(response)
    }
    //endregion

    private fun <T> send(rpc: JsonRpc<T>, handler: RpcHandler) {
        rpc.id = currentRpcId.addAndGet(1)

        rpcSocket.send(rpc)

        rpcHandlers[rpc.id] = handler
    }

    private fun <T> send(rpc: JsonRpc<T>, onSuccess: (T) -> Unit, onError: (Throwable) -> Unit) {
        try {
            val rpcHandler = RpcHandler(
                    onSuccess = { response ->
                        try {
                            onSuccess(rpc.parseResponse(response, gson))
                        } catch (error: Throwable) {
                            onError(error)
                        }
                    },
                    onError = onError
            )

            send(rpc, rpcHandler)
        } catch (error: Throwable) {
            onError(error)
        }
    }

    private fun <T> subscribe(subscription: RpcSubscription<T>, onSubscribeSuccess: () -> Unit, onSubscribeError: (Throwable) -> Unit, successHandler: (T) -> Unit, errorHandler: (Throwable) -> Unit) {
        send(
                rpc = SubscribeJsonRpc(subscription.params),
                onSuccess = { subscriptionId ->
                    subscriptionHandlers[subscriptionId] = { response ->
                        try {
                            successHandler(subscription.parse(response, gson))
                        } catch (error: Throwable) {
                            errorHandler(error)
                        }
                    }
                    onSubscribeSuccess()
                },
                onError = { error ->
                    onSubscribeError(error)
                }
        )
    }

    private fun subscribeToNewHeads() {
        subscribe(
                subscription = NewHeadsRpcSubscription(),
                onSubscribeSuccess = {
                },
                onSubscribeError = {
                },
                successHandler = { header ->
                    listener?.didUpdateLastBlockHeight(lastBlockHeight = header.number)
                },
                errorHandler = { error ->
                    logger.warning("NewHeads Handle Failed: ${error.javaClass.simpleName}")
                }
        )
    }

}
