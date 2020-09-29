package io.horizontalsystems.ethereumkit.api

import io.horizontalsystems.ethereumkit.api.jsonrpc.*
import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Single
import java.math.BigInteger
import java.util.*

class RpcBlockchain(
        private val address: Address,
        private val storage: IApiStorage,
        private val syncer: IRpcSyncer,
        private val transactionSigner: TransactionSigner,
        private val transactionBuilder: TransactionBuilder
) : IBlockchain, IRpcSyncerListener {

    //region IBlockchain
    override var listener: IBlockchainListener? = null

    override val source: String
        get() = "RPC ${syncer.source}"

    override val lastBlockHeight: Long?
        get() = storage.getLastBlockHeight()

    override val balance: BigInteger?
        get() = storage.getBalance()

    override val syncState: SyncState
        get() = syncer.syncState

    override fun start() {
        syncer.start()
    }

    override fun refresh() {
        syncer.refresh()
    }

    override fun stop() {
        syncer.stop()
    }

    override fun send(rawTransaction: RawTransaction): Single<Transaction> {
        return syncer.single(GetTransactionCountJsonRpc(address, DefaultBlockParameter.Pending))
                .flatMap { nonce ->
                    send(rawTransaction, nonce)
                }.doOnSuccess {
//                    sync() TODO is sync needed?
                }
    }

    private fun send(rawTransaction: RawTransaction, nonce: Long): Single<Transaction> {
        val signature = transactionSigner.signature(rawTransaction, nonce)
        val transaction = transactionBuilder.transaction(rawTransaction, nonce, signature)
        val encoded = transactionBuilder.encode(rawTransaction, nonce, signature)

        return syncer.single(SendRawTransactionJsonRpc(encoded))
                .map {
                    transaction
                }
    }

    override fun estimateGas(to: Address, amount: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: ByteArray?): Single<Long> {
        return syncer.single(EstimateGasJsonRpc(address, to, amount, gasLimit, gasPrice, data))
    }

    override fun getTransactionReceipt(transactionHash: ByteArray): Single<Optional<TransactionReceipt>> {
        return syncer.single(GetTransactionReceiptJsonRpc(transactionHash))
    }

    override fun getTransaction(transactionHash: ByteArray): Single<Optional<RpcTransaction>> {
        return syncer.single(GetTransactionByHashJsonRpc(transactionHash))
    }

    override fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>> {
        return syncer.single(GetLogsJsonRpc(address, DefaultBlockParameter.BlockNumber(fromBlock), DefaultBlockParameter.BlockNumber(toBlock), topics))
                .flatMap { logs ->
                    if (pullTimestamps) {
                        pullTransactionTimestamps(logs)
                    } else {
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
            requestSingles.add(syncer.single(GetBlockByNumberJsonRpc(blockNumber)))
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

    override fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        return syncer.single(GetStorageAtJsonRpc(contractAddress, position, defaultBlockParameter))
    }

    override fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray> {
        return syncer.single(CallJsonRpc(contractAddress, data, defaultBlockParameter))
    }
    //endregion

    //region IRpcSyncerListener
    override fun didUpdateSyncState(syncState: SyncState) {
        listener?.onUpdateSyncState(syncState)
    }

    override fun didUpdateLastBlockLogsBloom(lastBlockLogsBloom: String) {
        val bloomFilter = BloomFilter(lastBlockLogsBloom)

        if (bloomFilter.mayContainUserAddress(address)) {
            listener?.onUpdateLogsBloomFilter(bloomFilter)
        }
    }

    override fun didUpdateLastBlockHeight(lastBlockHeight: Long) {
        storage.saveLastBlockHeight(lastBlockHeight)
        listener?.onUpdateLastBlockHeight(lastBlockHeight)
    }

    override fun didUpdateBalance(balance: BigInteger) {
        storage.saveBalance(balance)
        listener?.onUpdateBalance(balance)
    }
    //endregion

    companion object {
        fun instance(address: Address,
                     storage: IApiStorage,
                     syncer: IRpcSyncer,
                     transactionSigner: TransactionSigner,
                     transactionBuilder: TransactionBuilder): RpcBlockchain {

            val rpcBlockchain = RpcBlockchain(address, storage, syncer, transactionSigner, transactionBuilder)
            syncer.listener = rpcBlockchain

            return rpcBlockchain
        }
    }
}
