package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.*
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.*
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class RpcBlockchain(
        private val address: Address,
        private val storage: IApiStorage,
        private val syncer: IRpcSyncer,
        private val transactionBuilder: TransactionBuilder
) : IBlockchain, IRpcSyncerListener {

    private val disposables = CompositeDisposable()

    private fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        storage.saveLastBlockHeight(lastBlockHeight)
        listener?.onUpdateLastBlockHeight(lastBlockHeight)
    }

    private fun onUpdateAccountState(state: AccountState) {
        storage.saveAccountState(state)
        listener?.onUpdateAccountState(state)
    }

    private fun syncLastBlockHeight() {
        syncer.single(BlockNumberJsonRpc())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ lastBlockNumber ->
                    onUpdateLastBlockHeight(lastBlockNumber)
                }, {
                    syncState = SyncState.NotSynced(it)
                }).let {
                    disposables.add(it)
                }
    }

    override fun syncAccountState() {
        Single.zip(
                syncer.single(GetBalanceJsonRpc(address, DefaultBlockParameter.Latest)),
                syncer.single(GetTransactionCountJsonRpc(address, DefaultBlockParameter.Latest))
        ) { t1, t2 -> Pair(t1, t2) }
                .subscribeOn(Schedulers.io())
                .subscribe({ (balance, nonce) ->
                    onUpdateAccountState(AccountState(balance, nonce))
                    syncState = SyncState.Synced()
                }, {
                    it?.printStackTrace()
                    syncState = SyncState.NotSynced(it)
                }).let {
                    disposables.add(it)
                }
    }


    //region IBlockchain
    override var syncState: SyncState = SyncState.NotSynced(EthereumKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onUpdateSyncState(value)
            }
        }

    override var listener: IBlockchainListener? = null

    override val source: String
        get() = "RPC ${syncer.source}"

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeight()

    override val accountState: AccountState?
        get() = storage.getAccountState()

    override fun start() {
        syncState = SyncState.Syncing()
        syncer.start()
    }

    override fun refresh() {
        when (syncer.state) {
            SyncerState.Preparing -> {
            }
            SyncerState.Ready -> {
                syncAccountState()
                syncLastBlockHeight()
            }
            is SyncerState.NotReady -> {
                syncer.start()
            }
        }
    }

    override fun stop() {
        syncer.stop()
    }

    override fun send(rawTransaction: RawTransaction, signature: Signature): Single<Transaction> {
        val transaction = transactionBuilder.transaction(rawTransaction, signature)
        val encoded = transactionBuilder.encode(rawTransaction, signature)

        return syncer.single(SendRawTransactionJsonRpc(encoded))
                .map { transaction }
    }

    override fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long> {
        return syncer.single(GetTransactionCountJsonRpc(address, defaultBlockParameter))
    }

    override fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice, data: ByteArray?): Single<Long> {
        return syncer.single(EstimateGasJsonRpc(address, to, amount, gasLimit, gasPrice, data))
    }

    override fun getTransactionReceipt(transactionHash: ByteArray): Single<RpcTransactionReceipt> {
        return syncer.single(GetTransactionReceiptJsonRpc(transactionHash))
    }

    override fun getTransaction(transactionHash: ByteArray): Single<RpcTransaction> {
        return syncer.single(GetTransactionByHashJsonRpc(transactionHash))
    }

    override fun getBlock(blockNumber: Long): Single<RpcBlock> {
        return syncer.single(GetBlockByNumberJsonRpc(blockNumber))
    }

    override fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>> {
        return syncer.single(GetLogsJsonRpc(address, DefaultBlockParameter.BlockNumber(fromBlock), DefaultBlockParameter.BlockNumber(toBlock), topics))
                .flatMap { logs ->
                    if (pullTimestamps) {
                        pullTransactionTimestamps(logs)
                    } else {
                        Single.just(logs)
                    }
                }
    }

    private fun pullTransactionTimestamps(logs: List<TransactionLog>): Single<List<TransactionLog>> {
        val logsByBlockNumber: MutableMap<Long, MutableList<TransactionLog>> = mutableMapOf()

        for (log in logs) {
            val logs: MutableList<TransactionLog> = logsByBlockNumber[log.blockNumber]
                    ?: mutableListOf()
            logs.add(log)
            logsByBlockNumber[log.blockNumber] = logs
        }

        val requestSingles: MutableList<Single<RpcBlock>> = mutableListOf()

        for ((blockNumber, _) in logsByBlockNumber) {
            requestSingles.add(syncer.single(GetBlockByNumberJsonRpc(blockNumber)))
        }

        return Single.merge(requestSingles).toList().map { blocks ->
            val resultLogs: MutableList<TransactionLog> = mutableListOf()

            for (block in blocks) {
                val logsOfBlock = logsByBlockNumber[block.number] ?: continue

                for (log in logsOfBlock) {
                    log.timestamp = block.timestamp
                    resultLogs.add(log)
                }
            }
            resultLogs
        }
    }

    override fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        return syncer.single(GetStorageAtJsonRpc(contractAddress, position, defaultBlockParameter))
    }

    override fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        return syncer.single(callRpc(contractAddress, data, defaultBlockParameter))
    }

    override fun <T> rpcSingle(rpc: JsonRpc<T>): Single<T> {
        return syncer.single(rpc)
    }

    //endregion

    //region IRpcSyncerListener
    override fun didUpdateLastBlockHeight(lastBlockHeight: Long) {
        onUpdateLastBlockHeight(lastBlockHeight)
    }

    override fun didUpdateSyncerState(state: SyncerState) {
        when (state) {
            SyncerState.Preparing -> {
                syncState = SyncState.Syncing()
            }
            SyncerState.Ready -> {
                syncState = SyncState.Syncing()
                syncAccountState()
                syncLastBlockHeight()
            }
            is SyncerState.NotReady -> {
                syncState = SyncState.NotSynced(state.error)
                disposables.clear()
            }
        }
    }

    //endregion

    companion object {
        fun instance(address: Address,
                     storage: IApiStorage,
                     syncer: IRpcSyncer,
                     transactionBuilder: TransactionBuilder): RpcBlockchain {

            val rpcBlockchain = RpcBlockchain(address, storage, syncer, transactionBuilder)
            syncer.listener = rpcBlockchain

            return rpcBlockchain
        }

        fun callRpc(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): DataJsonRpc =
            CallJsonRpc(contractAddress, data, defaultBlockParameter)
    }
}
