package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.spv.models.AccountStateSpv
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigInteger
import java.util.*


interface IApiStorage {
    fun getLastBlockHeight(): Long?
    fun saveLastBlockHeight(lastBlockHeight: Long)

    fun getAccountState(): AccountState?
    fun saveAccountState(state: AccountState)
}

interface ISpvStorage {
    fun getLastBlockHeader(): BlockHeader?
    fun saveBlockHeaders(blockHeaders: List<BlockHeader>)
    fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader>

    fun getAccountState(): AccountStateSpv?
    fun saveAccountSate(accountState: AccountStateSpv)
}

interface IBlockchain {
    val source: String
    var listener: IBlockchainListener?

    fun start()
    fun refresh()
    fun stop()
    fun syncAccountState()

    val syncState: EthereumKit.SyncState
    val lastBlockHeight: Long?
    val accountState: AccountState?

    fun send(rawTransaction: RawTransaction, signature: Signature): Single<Transaction>
    fun getNonce(): Single<Long>
    fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice, data: ByteArray?): Single<Long>
    fun getTransactionReceipt(transactionHash: ByteArray): Single<Optional<RpcTransactionReceipt>>
    fun getTransaction(transactionHash: ByteArray): Single<Optional<RpcTransaction>>
    fun getBlock(blockNumber: Long): Single<Optional<RpcBlock>>

    fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>>
    fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>
    fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>

    fun <T> rpcSingle(rpc: JsonRpc<T>): Single<T>
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)
    fun onUpdateSyncState(syncState: EthereumKit.SyncState)
    fun onUpdateAccountState(accountState: AccountState)
}

interface INotSyncedTransactionPool {
    val notSyncedTransactionsSignal: Flowable<Unit>

    fun add(notSyncedTransactions: List<NotSyncedTransaction>)
    fun remove(notSyncedTransaction: NotSyncedTransaction)
    fun update(notSyncedTransaction: NotSyncedTransaction)
    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction>
}

interface ITransactionSyncerStateStorage {
    fun getTransactionSyncerState(id: String): TransactionSyncerState?
    fun save(transactionSyncerState: TransactionSyncerState)
}

interface ITransactionStorage {
    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction>
    fun getNotSyncedInternalTransactions(): NotSyncedInternalTransaction?
    fun addNotSyncedTransactions(transactions: List<NotSyncedTransaction>)
    fun update(notSyncedTransaction: NotSyncedTransaction)
    fun remove(transaction: NotSyncedTransaction)

    fun getFullTransaction(hash: ByteArray): FullTransaction?
    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction>
    fun getTransactionHashes(): List<ByteArray>
    fun getTransactionsBeforeAsync(tags: List<List<String>>, hash: ByteArray?, limit: Int?): Single<List<FullTransaction>>
    fun getPendingTransactions(tags: List<List<String>>): List<FullTransaction>
    fun save(transaction: Transaction)

    fun saveInternalTransactions(internalTransactions: List<InternalTransaction>)
    fun add(notSyncedInternalTransaction: NotSyncedInternalTransaction)
    fun remove(notSyncedInternalTransaction: NotSyncedInternalTransaction)

    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?
    fun save(transactionReceipt: TransactionReceipt)

    fun save(logs: List<TransactionLog>)
    fun remove(logs: List<TransactionLog>)
    fun set(tags: List<TransactionTag>)

    fun getPendingTransactionList(nonce: Long): List<Transaction>
    fun getPendingTransactions(fromTransaction: Transaction?): List<Transaction>
    fun addDroppedTransaction(droppedTransaction: DroppedTransaction)
}

interface ITransactionSyncerListener {
    fun onTransactionsSynced(transactions: List<FullTransaction>)
}

interface ITransactionSyncer {
    val id: String
    val state: EthereumKit.SyncState
    val stateAsync: Flowable<EthereumKit.SyncState>

    fun start()
    fun stop()

    fun onEthereumKitSynced()
    fun onLastBlockBloomFilter(bloomFilter: BloomFilter)
    fun onUpdateAccountState(accountState: AccountState)
    fun onLastBlockNumber(blockNumber: Long)

    fun set(delegate: ITransactionSyncerDelegate)
}

interface ITransactionSyncerDelegate {
    val notSyncedTransactionsSignal: Flowable<Unit>

    fun getNotSyncedTransactions(limit: Int): List<NotSyncedTransaction>
    fun add(notSyncedTransactions: List<NotSyncedTransaction>)
    fun remove(notSyncedTransaction: NotSyncedTransaction)
    fun update(notSyncedTransaction: NotSyncedTransaction)

    fun getTransactionSyncerState(id: String): TransactionSyncerState?
    fun update(transactionSyncerState: TransactionSyncerState)
}

interface IDecorator {
    fun decorate(logs: List<TransactionLog>): List<ContractEventDecoration>
    fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): ContractMethodDecoration?
}

interface ITransactionWatcher {
    fun needInternalTransactions(fullTransaction: FullTransaction): Boolean
}

interface ITransactionProvider {
    fun getTransactions(startBlock: Long): Single<List<ProviderTransaction>>
    fun getInternalTransactions(startBlock: Long): Single<List<InternalTransaction>>
    fun getInternalTransactionsAsync(hash: ByteArray): Single<List<InternalTransaction>>
    fun getTokenTransactions(startBlock: Long): Single<List<ProviderTokenTransaction>>
}
