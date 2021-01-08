package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.BlockNumberJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetBalanceJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionCountJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.IRpcApiProvider
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ApiRpcSyncer(
        private val address: Address,
        private val rpcApiProvider: IRpcApiProvider,
        private val connectionManager: ConnectionManager
) : IRpcSyncer {
    private val disposables = CompositeDisposable()
    private var isStarted = false

    init {
        connectionManager.listener = object : ConnectionManager.Listener {
            override fun onConnectionChange() {
                sync()
            }
        }
    }

    //region IRpcSyncer
    override var listener: IRpcSyncerListener? = null
    override val source = "API ${rpcApiProvider.source}"
    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.didUpdateSyncState(value)
            }
        }

    override fun start() {
        isStarted = true

        sync()
    }

    override fun stop() {
        isStarted = false

        syncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        disposables.clear()
    }

    override fun refresh() {
        sync()
    }

    override fun <T> single(rpc: JsonRpc<T>): Single<T> {
        return rpcApiProvider.single(rpc)
    }
    //endregion

    private fun sync() {
        if (!isStarted) {
            return
        }
        if (!connectionManager.isConnected) {
            syncState = EthereumKit.SyncState.NotSynced(EthereumKit.SyncError.NoNetworkConnection())
            return
        }
        if (syncState is EthereumKit.SyncState.Syncing) {
            return
        }

        syncState = EthereumKit.SyncState.Syncing()

        Single.zip(
                rpcApiProvider.single(BlockNumberJsonRpc()),
                rpcApiProvider.single(GetBalanceJsonRpc(address, DefaultBlockParameter.Latest)),
                rpcApiProvider.single(GetTransactionCountJsonRpc(address, DefaultBlockParameter.Latest)),
                { t1, t2, t3 -> Triple(t1, t2, t3) })
                .subscribeOn(Schedulers.io())
                .subscribe({ (lastBlockNumber, balance, nonce) ->
                    listener?.didUpdateLastBlockHeight(lastBlockNumber)
                    listener?.didUpdateAccountState(AccountState(balance, nonce))

                    syncState = EthereumKit.SyncState.Synced()
                }, {
                    it?.printStackTrace()
                    syncState = EthereumKit.SyncState.NotSynced(it)
                }).let {
                    disposables.add(it)
                }
    }
}
