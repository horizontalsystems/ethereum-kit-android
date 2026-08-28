package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.BlockNumberJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.CallJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.DataJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.EstimateGasJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetBalanceJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetBlockByNumberJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetLogsJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetStorageAtJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionByHashJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionCountJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.GetTransactionReceiptJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.SendRawTransactionJsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.IApiStorage
import io.horizontalsystems.ethereumkit.core.IBlockchain
import io.horizontalsystems.ethereumkit.core.IBlockchainListener
import io.horizontalsystems.ethereumkit.core.INonceProvider
import io.horizontalsystems.ethereumkit.core.RpcApiProviderFactory
import io.horizontalsystems.ethereumkit.core.TransactionBuilder
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.RawTransaction
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.models.Signature
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.math.BigInteger

class RpcBlockchain(
    private val address: Address,
    private val storage: IApiStorage,
    private val syncer: IRpcSyncer,
    private val transactionBuilder: TransactionBuilder
) : IBlockchain, IRpcSyncerListener, INonceProvider {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun onUpdateLastBlockHeight(lastBlockHeight: Long) {
        storage.saveLastBlockHeight(lastBlockHeight)
        listener?.onUpdateLastBlockHeight(lastBlockHeight)
    }

    private fun onUpdateAccountState(state: AccountState) {
        storage.saveAccountState(state)
        listener?.onUpdateAccountState(state)
    }

    private fun syncLastBlockHeight() {
        scope.launch {
            try {
                val lastBlockNumber = syncer.execute(BlockNumberJsonRpc())
                onUpdateLastBlockHeight(lastBlockNumber)
            } catch (error: Throwable) {
                syncState = SyncState.NotSynced(error)
            }
        }
    }

    override fun syncAccountState() {
        scope.launch {
            try {
                val (balance, nonce) = coroutineScope {
                    val balance = async { syncer.execute(GetBalanceJsonRpc(address, DefaultBlockParameter.Latest)) }
                    val nonce = async { syncer.execute(GetTransactionCountJsonRpc(address, DefaultBlockParameter.Latest)) }
                    Pair(balance.await(), nonce.await())
                }
                onUpdateAccountState(AccountState(balance, nonce))
                syncState = SyncState.Synced()
            } catch (error: Throwable) {
                error.printStackTrace()
                syncState = SyncState.NotSynced(error)
            }
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

    override fun pause() {
        syncer.pause()
    }

    override fun resume() {
        syncer.resume()
    }

    override suspend fun send(rawTransaction: RawTransaction, signature: Signature): Transaction {
        val transaction = transactionBuilder.transaction(rawTransaction, signature)
        val encoded = transactionBuilder.encode(rawTransaction, signature)

        syncer.execute(SendRawTransactionJsonRpc(encoded))
        return transaction
    }

    override suspend fun getNonce(defaultBlockParameter: DefaultBlockParameter): Long {
        return syncer.execute(GetTransactionCountJsonRpc(address, defaultBlockParameter))
    }

    override suspend fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice?, data: ByteArray?): Long {
        return syncer.execute(EstimateGasJsonRpc(address, to, amount, gasLimit, gasPrice, data))
    }

    override suspend fun getTransactionReceipt(transactionHash: ByteArray): RpcTransactionReceipt {
        return syncer.execute(GetTransactionReceiptJsonRpc(transactionHash))
    }

    override suspend fun getTransaction(transactionHash: ByteArray): RpcTransaction {
        return syncer.execute(GetTransactionByHashJsonRpc(transactionHash))
    }

    override suspend fun getBlock(blockNumber: Long): RpcBlock {
        return syncer.execute(GetBlockByNumberJsonRpc(blockNumber))
    }

    override suspend fun getLogs(
        address: Address?,
        topics: List<ByteArray?>,
        fromBlock: Long,
        toBlock: Long,
        pullTimestamps: Boolean
    ): List<TransactionLog> {
        val logs = syncer.execute(
            GetLogsJsonRpc(
                address,
                DefaultBlockParameter.BlockNumber(fromBlock),
                DefaultBlockParameter.BlockNumber(toBlock),
                topics
            )
        )

        return if (pullTimestamps) {
            pullTransactionTimestamps(logs)
        } else {
            logs
        }
    }

    private suspend fun pullTransactionTimestamps(logs: List<TransactionLog>): List<TransactionLog> {
        val logsByBlockNumber: MutableMap<Long, MutableList<TransactionLog>> = mutableMapOf()

        for (log in logs) {
            val logs: MutableList<TransactionLog> = logsByBlockNumber[log.blockNumber]
                ?: mutableListOf()
            logs.add(log)
            logsByBlockNumber[log.blockNumber] = logs
        }

        val blocks = coroutineScope {
            logsByBlockNumber.keys.map { blockNumber ->
                async { syncer.execute(GetBlockByNumberJsonRpc(blockNumber)) }
            }.awaitAll()
        }

        val resultLogs: MutableList<TransactionLog> = mutableListOf()

        for (block in blocks) {
            val logsOfBlock = logsByBlockNumber[block.number] ?: continue

            for (log in logsOfBlock) {
                log.timestamp = block.timestamp
                resultLogs.add(log)
            }
        }
        return resultLogs
    }

    override suspend fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): ByteArray {
        return syncer.execute(GetStorageAtJsonRpc(contractAddress, position, defaultBlockParameter))
    }

    override suspend fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): ByteArray {
        return syncer.execute(callRpc(contractAddress, data, defaultBlockParameter))
    }

    override suspend fun <T: Any> rpc(rpc: JsonRpc<T>): T {
        return syncer.execute(rpc)
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
                scope.coroutineContext.cancelChildren()
            }
        }
    }

    //endregion

    companion object {
        fun instance(
            address: Address,
            storage: IApiStorage,
            syncer: IRpcSyncer,
            transactionBuilder: TransactionBuilder
        ): RpcBlockchain {

            val rpcBlockchain = RpcBlockchain(address, storage, syncer, transactionBuilder)
            syncer.listener = rpcBlockchain

            return rpcBlockchain
        }

        fun callRpc(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): DataJsonRpc =
            CallJsonRpc(contractAddress, data, defaultBlockParameter)

        suspend fun estimateGas(
            rpcSource: RpcSource,
            from: Address,
            to: Address?,
            amount: BigInteger?,
            gasLimit: Long?,
            gasPrice: GasPrice,
            data: ByteArray?
        ): Long {
            val rpcApiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)

            return rpcApiProvider.execute(EstimateGasJsonRpc(from, to, amount, gasLimit, gasPrice, data))
        }
    }
}
