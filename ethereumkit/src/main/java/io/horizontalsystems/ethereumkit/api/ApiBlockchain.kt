package io.horizontalsystems.ethereumkit.api

import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncError
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.horizontalsystems.ethereumkit.network.ConnectionManager
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class ApiBlockchain(
        private val storage: IApiStorage,
        private val rpcApiProvider: IRpcApiProvider,
        private val transactionSigner: TransactionSigner,
        private val transactionBuilder: TransactionBuilder,
        private val connectionManager: ConnectionManager)
    : IBlockchain {

    private val disposables = CompositeDisposable()

    init {
        connectionManager.listener = object : ConnectionManager.Listener {
            override fun onConnectionChange(isConnected: Boolean) {
                sync()
            }
        }
    }

    override val source: String
        get() = "RPC (${rpcApiProvider.source})"

    override var listener: IBlockchainListener? = null

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeight()

    override val balance: BigInteger?
        get() = storage.getBalance()

    override var syncState: SyncState = SyncState.NotSynced(SyncError.NotStarted())
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateSyncState(value)
            }
        }

    override fun start() {
        sync()
    }

    override fun refresh() {
        sync()
    }

    override fun stop() {
        disposables.clear()
    }

    override fun send(rawTransaction: RawTransaction): Single<EthereumTransaction> {
        return rpcApiProvider.getTransactionCount()
                .flatMap { nonce ->
                    send(rawTransaction, nonce)
                }.doOnSuccess {
                    sync()
                }
    }

    private fun send(rawTransaction: RawTransaction, nonce: Long): Single<EthereumTransaction> {
        val signature = transactionSigner.signature(rawTransaction, nonce)
        val transaction = transactionBuilder.transaction(rawTransaction, nonce, signature)
        val encoded = transactionBuilder.encode(rawTransaction, nonce, signature)

        return rpcApiProvider.send(signedTransaction = encoded)
                .map {
                    transaction
                }
    }

    override fun estimateGas(from: String?, to: String, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: ByteArray?): Single<Long> {
        return rpcApiProvider.estimateGas(from, to, value, gasLimit, gasPrice, data?.toHexString())
                .flatMap {
                    Single.just(BigInteger(it.replace("0x", ""), 16).toLong())
                }
    }

    override fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus> {
        return rpcApiProvider.transactionReceiptStatus(transactionHash)
    }

    override fun transactionExist(transactionHash: ByteArray): Single<Boolean> {
        return rpcApiProvider.transactionExist(transactionHash)
    }

    override fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
        return rpcApiProvider.getLogs(address, fromBlock, toBlock, topics)
                .flatMap { logs ->
                    if (pullTimestamps)
                        pullTransactionTimestamps(logs)
                    else {
                        Single.just(logs)
                    }
                }
    }

    private fun pullTransactionTimestamps(ethereumLogs: List<EthereumLog>): Single<List<EthereumLog>> {
        val logsByBlockNumber: MutableMap<Long, MutableList<EthereumLog>> = mutableMapOf()

        for (log in ethereumLogs) {
            val logs: MutableList<EthereumLog> = logsByBlockNumber[log.blockNumber]
                    ?: mutableListOf()
            logs.add(log)
            logsByBlockNumber[log.blockNumber] = logs
        }

        val requestSingles: MutableList<Single<Block>> = mutableListOf()

        for ((blockNumber, _) in logsByBlockNumber) {
            requestSingles.add(rpcApiProvider.getBlock(blockNumber))
        }

        return Single.merge(requestSingles).toList().map { blocks ->
            val resultLogs: MutableList<EthereumLog> = mutableListOf()

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

    override fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray> {
        return rpcApiProvider.getStorageAt(contractAddress, position.toHexString(), blockNumber)
                .map { it.hexStringToByteArray() }
    }

    override fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<ByteArray> {
        return rpcApiProvider.call(contractAddress, data, blockNumber).flatMap<ByteArray> { value ->
            val rawValue = try {
                value.hexStringToByteArray()
            } catch (ex: Exception) {
                return@flatMap Single.error(ApiException.InvalidData())
            }
            Single.just(rawValue)
        }
    }

    private fun sync() {
        if (!connectionManager.isConnected) {
            syncState = SyncState.NotSynced(SyncError.NoNetworkConnection())
            return
        }

        if (syncState is SyncState.Syncing) {
            return
        }
        syncState = SyncState.Syncing()

        Single.zip(
                rpcApiProvider.getLastBlockHeight(),
                rpcApiProvider.getBalance(),
                BiFunction<Long, BigInteger, Pair<Long, BigInteger>> { t1, t2 -> Pair(t1, t2) })
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    updateLastBlockHeight(result.first)
                    updateBalance(result.second)

                    syncState = SyncState.Synced()
                }, {
                    it?.printStackTrace()
                    syncState = SyncState.NotSynced(it)
                }).let {
                    disposables.add(it)
                }
    }

    private fun updateLastBlockHeight(height: Long) {
        storage.saveLastBlockHeight(height)
        listener?.onUpdateLastBlockHeight(height)
    }

    private fun updateBalance(balance: BigInteger) {
        storage.saveBalance(balance)
        listener?.onUpdateBalance(balance)
    }

    open class ApiException : Exception() {
        class InvalidData : ApiException()
    }

    companion object {
        fun getInstance(storage: IApiStorage,
                        transactionSigner: TransactionSigner,
                        transactionBuilder: TransactionBuilder,
                        rpcApiProvider: IRpcApiProvider,
                        connectionManager: ConnectionManager): ApiBlockchain {

            return ApiBlockchain(storage, rpcApiProvider, transactionSigner, transactionBuilder, connectionManager)
        }
    }
}
