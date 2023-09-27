package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import com.tinder.scarlet.Event
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.messageadapter.gson.GsonMessageAdapter
import com.tinder.scarlet.retry.ExponentialWithJitterBackoffStrategy
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URI
import java.util.logging.Logger

class NodeWebSocket(
    uri: URI,
    private val gson: Gson,
    auth: String? = null
) : IRpcWebSocket {
    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private var disposables = CompositeDisposable()

    private val RETRY_BASE_DURATION: Long = 3000
    private val RETRY_MAX_DURATION: Long = 5000

    private val scarlet: Scarlet
    private var socket: WebSocketService? = null

    private var state: WebSocketState = WebSocketState.Disconnected(WebSocketState.DisconnectError.NotStarted)
        set(value) {
            field = value
            listener?.didUpdate(value)
        }

    init {
        val backoffStrategy = ExponentialWithJitterBackoffStrategy(RETRY_BASE_DURATION, RETRY_MAX_DURATION)

        val loggingInterceptor = HttpLoggingInterceptor(
                object : HttpLoggingInterceptor.Logger {
                    override fun log(message: String) {
                        logger.info(message)
                    }
                })
                .setLevel(HttpLoggingInterceptor.Level.BASIC)

        val headersInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            auth?.let {
                requestBuilder.header("Authorization", Credentials.basic("", auth))
            }
            requestBuilder.header("Content-Type", "application/json")
            requestBuilder.header("Accept", "application/json")
            chain.proceed(requestBuilder.build())
        }

        val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(headersInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

        scarlet = Scarlet.Builder()
                .webSocketFactory(okHttpClient.newWebSocketFactory(uri.toString()))
                .addMessageAdapterFactory(GsonMessageAdapter.Factory(gson))
                .addStreamAdapterFactory(RxJava2StreamAdapterFactory())
                .backoffStrategy(backoffStrategy)
                .build()
    }

    //region IRpcWebSocket
    override var listener: IRpcWebSocketListener? = null

    override val source: String = uri.host

    override fun start() {
        state = WebSocketState.Connecting

        connect()
    }

    override fun stop() {
        disconnect()
    }

    override fun <T> send(rpc: JsonRpc<T>) {
        logger.info("Sending ${gson.toJson(rpc)}")

        check(state == WebSocketState.Connected) {
            throw SocketError.NotConnected
        }
        socket?.send(rpc)
    }
    //endregion

    private fun connect() {
        if (socket == null) {
            scarlet.create<WebSocketService>().apply {
                socket = this
                observeSocket(this)
            }
        }
    }

    private fun disconnect() {
        disposables.clear()
    }

    private fun observeSocket(socket: WebSocketService) {
        socket.observeEvents()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ event ->
                    when (event) {
                        is Event.OnWebSocket.Event<*> -> when (val webSocketEvent = event.event) {
                            is WebSocket.Event.OnConnectionOpened<*> -> {
                                logger.info("On WebSocket Connection Opened")
                                state = WebSocketState.Connected
                            }
                            is WebSocket.Event.OnMessageReceived -> {
//                                logger.info("On WebSocket Message Received: ${webSocketEvent.message}")
                            }
                            is WebSocket.Event.OnConnectionClosing -> {
                                logger.info("On WebSocket Connection Closing")
                            }
                            is WebSocket.Event.OnConnectionClosed -> {
                                logger.info("On WebSocket Connection Closed")

                                state = WebSocketState.Disconnected(WebSocketState.DisconnectError.SocketDisconnected(webSocketEvent.shutdownReason.reason))
                            }
                            is WebSocket.Event.OnConnectionFailed -> {
                                logger.info("On WebSocket Connection Failed")

                                state = WebSocketState.Disconnected(webSocketEvent.throwable)

                                webSocketEvent.throwable.printStackTrace()
                            }
                        }
                        Event.OnWebSocket.Terminate -> {
                            logger.info("On WebSocket Terminate")
                        }
                        is Event.OnStateChange<*> -> {
                            event.state
                            logger.info("On State Change: ${event.state.javaClass.simpleName}")
                        }
                        Event.OnRetry -> {
                            logger.info("On Retry")
                        }
                        is Event.OnLifecycle -> {
                            logger.info("On LifeCycle: $event")
                        }
                        else -> {
                            logger.info("On Event: $event")
                        }
                    }
                }, { error ->
                    error.printStackTrace()
                    logger.warning(error.message)
                })
                .let { disposables.add(it) }

        socket.observeResponse()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ response ->
                    logger.info("On Response: $response")
                    try {
                        when {
                            response.id != null -> {
                                listener?.didReceive(RpcResponse(response.id, response.result, response.error))
                            }
                            response.method == "eth_subscription" && response.params != null -> {
                                listener?.didReceive(RpcSubscriptionResponse(response.method, response.params))
                            }
                            else -> {
                                logger.warning("Unknown Response: $response")
                            }
                        }
                    } catch (error: Throwable) {
                        logger.warning("Handle Response error: ${error.javaClass.simpleName}")
                        error.printStackTrace()
                    }
                }, { error ->
                    logger.warning("On Response error: ${error.message ?: error.javaClass.simpleName}")
                    error.printStackTrace()
                })
                .let { disposables.add(it) }
    }

    private interface WebSocketService {
        @Send
        fun send(rpc: JsonRpc<*>)

        @Receive
        fun observeEvents(): Flowable<Event>

        @Receive
        fun observeResponse(): Flowable<RpcGeneralResponse>
    }

    sealed class SocketError : Throwable() {
        object NotConnected : SocketError()
    }

}
