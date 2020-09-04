package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Single
import java.math.BigInteger


interface IApiStorage {
    fun getLastBlockHeight(): Long?
    fun saveLastBlockHeight(lastBlockHeight: Long)

    fun getBalance(): BigInteger?
    fun saveBalance(balance: BigInteger)
}

interface ISpvStorage {
    fun getLastBlockHeader(): BlockHeader?
    fun saveBlockHeaders(blockHeaders: List<BlockHeader>)
    fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader>

    fun getAccountState(): AccountState?
    fun saveAccountSate(accountState: AccountState)
}

interface IBlockchain {
    val source: String
    var listener: IBlockchainListener?

    fun start()
    fun refresh()
    fun stop()

    val syncState: EthereumKit.SyncState
    val lastBlockHeight: Long?
    val balance: BigInteger?

    fun send(rawTransaction: RawTransaction): Single<Transaction>
    fun estimateGas(to: Address, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: ByteArray?): Single<Long>
    fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus>
    fun transactionExist(transactionHash: ByteArray): Single<Boolean>

    fun getLogs(address: Address?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>>
    fun getStorageAt(contractAddress: Address, position: ByteArray, blockNumber: Long): Single<ByteArray>
    fun call(contractAddress: Address, data: ByteArray, blockNumber: Long?): Single<ByteArray>
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)

    fun onUpdateBalance(balance: BigInteger)
    fun onUpdateSyncState(syncState: EthereumKit.SyncState)
}

interface IRpcApiProvider {
    val source: String

    fun getLastBlockHeight(): Single<Long>
    fun getTransactionCount(): Single<Long>

    fun getBalance(): Single<BigInteger>

    fun send(signedTransaction: ByteArray): Single<Unit>

    fun getStorageAt(contractAddress: Address, position: String, blockNumber: Long?): Single<String>
    fun getLogs(address: Address?, fromBlock: Long, toBlock: Long, topics: List<ByteArray?>): Single<List<EthereumLog>>
    fun getBlock(blockNumber: Long): Single<Block>
    fun call(contractAddress: Address, data: ByteArray, blockNumber: Long?): Single<String>
    fun estimateGas(to: Address, value: BigInteger?, gasLimit: Long?, gasPrice: Long?, data: String?): Single<Long>
    fun transactionReceiptStatus(transactionHash: ByteArray): Single<TransactionStatus>
    fun transactionExist(transactionHash: ByteArray): Single<Boolean>
}

interface ITransactionManager {
    val syncState: EthereumKit.SyncState
    val source: String
    var listener: ITransactionManagerListener?

    fun refresh()
    fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionWithInternal>>
    fun handle(transaction: Transaction)
}

interface ITransactionManagerListener {
    fun onUpdateTransactions(transactions: List<TransactionWithInternal>)
    fun onUpdateTransactionsSyncState(syncState: EthereumKit.SyncState)
}

interface ITransactionsProvider {
    val source: String

    fun getTransactions(startBlock: Long): Single<List<Transaction>>
    fun getInternalTransactions(startBlock: Long): Single<List<InternalTransaction>>
}

interface ITransactionStorage {
    fun getLastTransactionBlockHeight(): Long?
    fun getLastInternalTransactionBlockHeight(): Long?

    fun saveTransactions(transactions: List<Transaction>)
    fun saveInternalTransactions(transactions: List<InternalTransaction>)
    fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionWithInternal>>
}
