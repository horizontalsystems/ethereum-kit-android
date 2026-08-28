package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.BlockNumberJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.schedule

class ApiRpcSyncer(
    private val rpcApiProvider: IRpcApiProvider,
    private val connectionManager: ConnectionManager,
    private val syncInterval: Long,
) : IRpcSyncer {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isStarted = false
    private var timer: Timer? = null

    init {
        connectionManager.listener = object : ConnectionManager.Listener {
            override fun onConnectionChange() {
                handleConnectionChange()
            }
        }
    }

    //region IRpcSyncer
    override var listener: IRpcSyncerListener? = null
    override val source = "API ${rpcApiProvider.source}"
    override var state: SyncerState = SyncerState.NotReady(EthereumKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.didUpdateSyncerState(value)
            }
        }

    override fun start() {
        isStarted = true

        handleConnectionChange()
    }

    override fun stop() {
        isStarted = false

        state = SyncerState.NotReady(EthereumKit.SyncError.NotStarted())
        scope.coroutineContext.cancelChildren()
        stopTimer()
    }

    override fun pause() {
        stopTimer()
    }

    override fun resume() {
        startTimer()
    }

    override suspend fun <T: Any> execute(rpc: JsonRpc<T>): T =
        rpcApiProvider.execute(rpc)
    //endregion

    private fun handleConnectionChange() {
        if (!isStarted) return

        if (connectionManager.isConnected) {
            state = SyncerState.Ready
            startTimer()
        } else {
            state = SyncerState.NotReady(EthereumKit.SyncError.NoNetworkConnection())
            stopTimer()
        }
    }

    private fun startTimer() {
        if (timer != null) return

        timer = Timer().apply {
            schedule(0, syncInterval * 1000) {
                onFireTimer()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun onFireTimer() {
        scope.launch {
            try {
                val lastBlockNumber = rpcApiProvider.execute(BlockNumberJsonRpc())
                listener?.didUpdateLastBlockHeight(lastBlockNumber)
            } catch (error: Throwable) {
                // ignored, same as the previous RxJava subscription without an error handler
            }
        }
    }

}
