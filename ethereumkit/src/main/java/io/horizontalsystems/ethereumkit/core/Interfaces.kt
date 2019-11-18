package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Block
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
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

    fun send(rawTransaction: RawTransaction): Single<EthereumTransaction>
    fun estimateGas(from: String?, to: String, value: BigInteger?, gasLimit: Int?, data: ByteArray?): Single<Int>

    fun getLogs(address: ByteArray?, topics: List<ByteArray?>, fromBlock: Long, toBlock: Long, pullTimestamps: Boolean): Single<List<EthereumLog>>
    fun getStorageAt(contractAddress: ByteArray, position: ByteArray, blockNumber: Long): Single<ByteArray>
    fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<ByteArray>
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

    fun getStorageAt(contractAddress: ByteArray, position: String, blockNumber: Long?): Single<String>
    fun getLogs(address: ByteArray?, fromBlock: Long?, toBlock: Long?, topics: List<ByteArray?>): Single<List<EthereumLog>>
    fun getBlock(blockNumber: Long): Single<Block>
    fun call(contractAddress: ByteArray, data: ByteArray, blockNumber: Long?): Single<String>
    fun estimateGas(from: String?, to: String, value: BigInteger?, gasLimit:Int?, data: String?): Single<String>

}
interface ITransactionManager {
    val source: String
    var listener: ITransactionManagerListener?

    fun refresh()
    fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>>
    fun handle(transaction: EthereumTransaction)
}

interface ITransactionManagerListener {
    fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>)
}

interface ITransactionsProvider {
    val source: String

    fun getTransactions(startBlock: Long): Single<List<EthereumTransaction>>
}

interface ITransactionStorage {
    fun getLastTransactionBlockHeight(): Long?

    fun saveTransactions(transactions: List<EthereumTransaction>)
    fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>>
}
