package io.horizontalsystems.ethereumkit.api.core

import com.google.gson.Gson
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import kotlin.math.min
import kotlin.random.Random

/**
 * JSON-RPC transport over a plain OkHttp [WebSocket].
 *
 * Reconnects automatically after an unexpected close or failure, using exponential backoff with
 * jitter between [RETRY_BASE_DURATION] and [RETRY_MAX_DURATION] milliseconds, until [stop] is called.
 */
class NodeWebSocket(
    uri: URI,
    private val gson: Gson,
    auth: String? = null
) : IRpcWebSocket {
    private val logger = Logger.getLogger(this.javaClass.simpleName)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val RETRY_BASE_DURATION: Long = 3000
    private val RETRY_MAX_DURATION: Long = 5000

    private val okHttpClient: OkHttpClient
    private val request: Request

    private var socket: WebSocket? = null
    private var reconnectJob: Job? = null
    private val retryCount = AtomicInteger(0)

    @Volatile
    private var isStarted = false

    private var state: WebSocketState = WebSocketState.Disconnected(WebSocketState.DisconnectError.NotStarted)
        set(value) {
            field = value
            listener?.didUpdate(value)
        }

    init {
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

        okHttpClient = OkHttpClient.Builder()
                .addInterceptor(headersInterceptor)
                .addInterceptor(loggingInterceptor)
                .pingInterval(30, TimeUnit.SECONDS)
                .build()

        request = Request.Builder()
                .url(uri.toString())
                .build()
    }

    //region IRpcWebSocket
    override var listener: IRpcWebSocketListener? = null

    override val source: String = uri.host

    override fun start() {
        if (isStarted) return
        isStarted = true

        state = WebSocketState.Connecting
        retryCount.set(0)

        connect()
    }

    override fun stop() {
        isStarted = false

        reconnectJob?.cancel()
        reconnectJob = null

        disconnect()
    }

    override fun <T> send(rpc: JsonRpc<T>) {
        val json = gson.toJson(rpc)
        logger.info("Sending $json")

        check(state == WebSocketState.Connected) {
            throw SocketError.NotConnected
        }
        socket?.send(json)
    }
    //endregion

    @Synchronized
    private fun connect() {
        if (socket != null) return

        socket = okHttpClient.newWebSocket(request, socketListener)
    }

    @Synchronized
    private fun disconnect() {
        socket?.close(NORMAL_CLOSURE_CODE, null)
        socket = null

        if (state !is WebSocketState.Disconnected) {
            state = WebSocketState.Disconnected(WebSocketState.DisconnectError.NotStarted)
        }
    }

    private fun scheduleReconnect() {
        if (!isStarted) return
        if (reconnectJob?.isActive == true) return

        val attempt = retryCount.getAndIncrement()
        val exponential = min(RETRY_MAX_DURATION, RETRY_BASE_DURATION * (1L shl min(attempt, 10)))
        val jittered = exponential / 2 + Random.nextLong(exponential / 2 + 1)

        logger.info("On Retry (attempt ${attempt + 1}, in ${jittered}ms)")

        reconnectJob = scope.launch {
            delay(jittered)
            if (!isStarted) return@launch

            state = WebSocketState.Connecting
            connect()
        }
    }

    private fun handleMessage(text: String) {
        try {
            val response = gson.fromJson(text, RpcGeneralResponse::class.java)
            logger.info("On Response: $response")

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
    }

    private val socketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            logger.info("On WebSocket Connection Opened")

            retryCount.set(0)
            state = WebSocketState.Connected
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            logger.info("On WebSocket Connection Closing")
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            logger.info("On WebSocket Connection Closed")

            synchronized(this@NodeWebSocket) {
                if (socket === webSocket) socket = null
            }

            if (!isStarted) return

            state = WebSocketState.Disconnected(WebSocketState.DisconnectError.SocketDisconnected(reason))
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            logger.info("On WebSocket Connection Failed")

            synchronized(this@NodeWebSocket) {
                if (socket === webSocket) socket = null
            }

            if (!isStarted) return

            state = WebSocketState.Disconnected(t)
            t.printStackTrace()
            scheduleReconnect()
        }
    }

    sealed class SocketError : Throwable() {
        object NotConnected : SocketError()
    }

    companion object {
        private const val NORMAL_CLOSURE_CODE = 1000
    }

}
