package io.horizontalsystems.ethereumkit.api

import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger

class ApiBlockchain(
        private val storage: IApiStorage,
        private val transactionsProvider: ITransactionsProvider,
        private val rpcApiProvider: IRpcApiProvider,
        private val transactionSigner: TransactionSigner,
        private val transactionBuilder: TransactionBuilder,
        override val address: ByteArray
) : IBlockchain {

    private val disposables = CompositeDisposable()
    private var started = false

    override var listener: IBlockchainListener? = null

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeight()

    override val balance: BigInteger?
        get() = storage.getBalance(address)

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateSyncState(value)
            }
        }

    override fun start() {
        started = true
        sync()
    }

    override fun refresh() {
        sync()
    }

    override fun stop() {
        started = false
        disposables.clear()
    }

    override fun clear() {
        disposables.clear()
        storage.clear()
    }

    override fun send(rawTransaction: RawTransaction): Single<EthereumTransaction> {
        return rpcApiProvider.getTransactionCount(address)
                .flatMap { nonce ->
                    send(rawTransaction, nonce)
                }.doOnSuccess { transaction ->
                    updateTransactions(listOf(transaction))
                    sync()
                }
    }

    private fun send(rawTransaction: RawTransaction, nonce: Long): Single<EthereumTransaction> {
        val signature = transactionSigner.sign(rawTransaction, nonce)
        val transaction = transactionBuilder.transaction(rawTransaction, nonce, signature, address)
        val encoded = transactionBuilder.encode(rawTransaction, nonce, signature)

        return rpcApiProvider.send(signedTransaction = encoded)
                .map {
                    transaction
                }
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
        if (!started) {
            return
        }

        if (syncState == EthereumKit.SyncState.Syncing) {
            return
        }
        syncState = EthereumKit.SyncState.Syncing

        Single.zip(
                rpcApiProvider.getLastBlockHeight(),
                rpcApiProvider.getBalance(address),
                BiFunction<Long, BigInteger, Pair<Long, BigInteger>> { t1, t2 -> Pair(t1, t2) })
                .subscribeOn(Schedulers.io())
                .subscribe({ result ->
                    updateLastBlockHeight(result.first)
                    updateBalance(result.second)
                    refreshTransactions()
                }, {
                    it?.printStackTrace()
                    syncState = EthereumKit.SyncState.NotSynced
                }).let {
                    disposables.add(it)
                }
    }

    private fun refreshTransactions() {
        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight(false) ?: 0

        transactionsProvider.getTransactions(address, (lastTransactionBlockHeight + 1))
                .subscribeOn(Schedulers.io())
                .subscribe({ transactions ->
                    updateTransactions(transactions)
                    syncState = EthereumKit.SyncState.Synced
                }, {
                    syncState = EthereumKit.SyncState.NotSynced
                })?.let {
                    disposables.add(it)
                }
    }

    private fun updateLastBlockHeight(height: Long) {
        storage.saveLastBlockHeight(height)
        listener?.onUpdateLastBlockHeight(height)
    }

    private fun updateBalance(balance: BigInteger) {
        storage.saveBalance(balance, address)
        listener?.onUpdateBalance(balance)
    }

    private fun updateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions.filter { it.input.isEmpty() })
    }

    open class ApiException : Exception() {
        class InvalidData : ApiException()
    }

    companion object {
        fun getInstance(storage: IApiStorage,
                        networkType: NetworkType,
                        transactionSigner: TransactionSigner,
                        transactionBuilder: TransactionBuilder,
                        address: ByteArray,
                        infuraApiKey: String,
                        etherscanApiKey: String): ApiBlockchain {

            val rpcProvider = RpcApiProvider.getInstance(networkType, infuraApiKey)
            val transactionsProvider = TransactionsProvider.getInstance(networkType, etherscanApiKey)

            return ApiBlockchain(storage, transactionsProvider, rpcProvider, transactionSigner, transactionBuilder, address)
        }
    }

}
