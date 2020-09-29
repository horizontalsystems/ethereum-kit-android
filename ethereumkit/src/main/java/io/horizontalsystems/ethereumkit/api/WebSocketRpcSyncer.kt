package io.horizontalsystems.ethereumkit.api

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.BlockNumberJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetBalanceJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.SubscribeJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpcsubscription.NewHeadsRpcSubscription
import io.horizontalsystems.ethereumkit.api.jsonrpcsubscription.RpcSubscription
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.reactivex.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger

class WebSocketRpcSyncer(
        private val address: Address,
        private val rpcSocket: IRpcWebSocket,
        private val gson: Gson
) : IRpcSyncer, IRpcWebSocketListener {
    private val logger = Logger.getLogger("WebSocketRpcSyncer")

    private var currentRpcId = AtomicInteger(0)
    private var rpcHandlers = ConcurrentHashMap<Int, RpcHandler>()
    private var subscriptionHandlers = ConcurrentHashMap<Long, SubscriptionHandler>()

    private var isSubscribedToNewHeads = false

    //region IRpcSyncer
    override var listener: IRpcSyncerListener? = null

    override val source = "WebSocket ${rpcSocket.source}"

    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.didUpdateSyncState(value)
            }
        }

    override fun start() {
        syncState = EthereumKit.SyncState.Syncing()

        rpcSocket.start()
    }

    override fun stop() {
        syncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())

        rpcSocket.stop()
    }

    override fun refresh() {} // N/A

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
    override fun didUpdate(state: WebSocketState) {
        if ((syncState as? EthereumKit.SyncState.NotSynced)?.error is EthereumKit.SyncError.NotStarted) {
            return
        }

        when (state) {
            WebSocketState.Connecting -> {
                syncState = EthereumKit.SyncState.Syncing()
            }
            WebSocketState.Connected -> {
                fetchLastBlockHeight()
                subscribeToNewHeads()
            }
            is WebSocketState.Disconnected -> {
                rpcHandlers.forEach { rpcHandlers.remove(it.key) }
                subscriptionHandlers.forEach { subscriptionHandlers.remove(it.key) }

                isSubscribedToNewHeads = false
                syncState = EthereumKit.SyncState.NotSynced(state.error)
            }
        }
    }

    override fun didReceive(response: RpcResponse) {
        rpcHandlers.remove(response.id)?.invoke(response)
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
            send(rpc) { response ->
                try {
                    onSuccess(rpc.parseResponse(response, gson))
                } catch (error: Throwable) {
                    onError(error)
                }
            }
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

    private fun fetchLastBlockHeight() {
        send(
                rpc = BlockNumberJsonRpc(),
                onSuccess = { lastBlockHeight ->
                    listener?.didUpdateLastBlockHeight(lastBlockHeight)
                    fetchBalance()
                },
                onError = { error ->
                    onFailSync(error)
                }
        )
    }

    private fun fetchBalance() {
        send(
                rpc = GetBalanceJsonRpc(address, DefaultBlockParameter.Latest),
                onSuccess = { balance ->
                    listener?.didUpdateBalance(balance)
                    syncState = EthereumKit.SyncState.Synced()
                },
                onError = { error ->
                    onFailSync(error)
                }
        )
    }

    private fun subscribeToNewHeads() {
        if (isSubscribedToNewHeads)
            return

        subscribe(
                subscription = NewHeadsRpcSubscription(),
                onSubscribeSuccess = {
                    isSubscribedToNewHeads = true
                },
                onSubscribeError = { error ->
                    isSubscribedToNewHeads = false
                    onFailSync(error)
                },
                successHandler = { header ->
                    listener?.didUpdateLastBlockLogsBloom(header.logsBloom)
                    listener?.didUpdateLastBlockHeight(lastBlockHeight = header.number)
                    fetchBalance()
                },
                errorHandler = { error ->
                    error.printStackTrace()
                    logger.warning("NewHeads Handle Failed: ${error.javaClass.simpleName}")
                }
        )
    }

    private fun onFailSync(error: Throwable) {
        syncState = EthereumKit.SyncState.NotSynced(error)
    }
}
