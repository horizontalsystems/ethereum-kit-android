package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcBlock
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransactionReceipt
import io.horizontalsystems.ethereumkit.api.models.AccountState
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.spv.models.AccountStateSpv
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
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

    val syncState: EthereumKit.SyncState
    val lastBlockHeight: Long?
    val accountState: AccountState?

    fun send(rawTransaction: RawTransaction): Single<Transaction>
    fun getNonce(): Single<Long>
    fun estimateGas(to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: ByteArray?): Single<Long>
    fun getTransactionReceipt(transactionHash: ByteArray): Single<Optional<RpcTransactionReceipt>>
    fun getTransaction(transactionHash: ByteArray): Single<Optional<RpcTransaction>>
    fun getBlock(blockNumber: Long): Single<Optional<RpcBlock>>

    fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<TransactionLog>>
    fun getStorageAt(contractAddress: Address, position: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>
    fun call(contractAddress: Address, data: ByteArray, defaultBlockParameter: DefaultBlockParameter): Single<ByteArray>
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
    fun addNotSyncedTransactions(transactions: List<NotSyncedTransaction>)
    fun update(notSyncedTransaction: NotSyncedTransaction)
    fun remove(transaction: NotSyncedTransaction)

    fun getFullTransaction(hash: ByteArray): FullTransaction?
    fun getFullTransactions(hashes: List<ByteArray>): List<FullTransaction>
    fun getFullTransactions(fromSyncOrder: Long?): List<FullTransaction>
    fun getTransactionHashes(): List<ByteArray>
    fun getEtherTransactionsAsync(address: Address, fromHash: ByteArray?, limit: Int?): Single<List<FullTransaction>>
    fun save(transaction: Transaction)

    fun getLastInternalTransactionBlockHeight(): Long?
    fun saveInternalTransactions(internalTransactions: List<InternalTransaction>)

    fun getTransactionReceipt(transactionHash: ByteArray): TransactionReceipt?
    fun save(transactionReceipt: TransactionReceipt)

    fun save(logs: List<TransactionLog>)

    fun getPendingTransaction(nonce: Long): Transaction?
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

sealed class TransactionDecoration {
    class Transfer(
            val from: Address,
            val to: Address,
            val value: BigInteger
    ) : TransactionDecoration()

    class Eip20Transfer(
            val to: Address,
            val value: BigInteger,
            val contractAddress: Address
    ) : TransactionDecoration()

    class Eip20Approve(
            val spender: Address,
            val value: BigInteger,
            val contractAddress: Address
    ) : TransactionDecoration()

    class Swap(
            val trade: Trade,
            val tokenIn: Token,
            val tokenOut: Token,
            val to: Address,
            val deadline: BigInteger
    ) : TransactionDecoration() {

        sealed class Trade {
            class ExactIn(val amountIn: BigInteger, val amountOutMin: BigInteger): Trade()
            class ExactOut(val amountOut: BigInteger, val amountInMax: BigInteger): Trade()
        }

        sealed class Token {
            object EvmCoin: Token()
            class Eip20Coin(val address: Address): Token()
        }
    }

}

interface IDecorator {
    fun decorate(transactionData: TransactionData): TransactionDecoration?
}
