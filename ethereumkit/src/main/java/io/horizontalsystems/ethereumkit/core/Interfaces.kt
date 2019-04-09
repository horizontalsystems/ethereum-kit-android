package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Single
import java.math.BigInteger


interface IApiStorage {
    fun getLastBlockHeight(): Long?
    fun getBalance(address: ByteArray): BigInteger?
    fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>>

    fun getLastTransactionBlockHeight(isErc20: Boolean): Long?
    fun saveLastBlockHeight(lastBlockHeight: Long)
    fun saveBalance(balance: BigInteger, address: ByteArray)
    fun saveTransactions(ethereumTransactions: List<EthereumTransaction>)

    fun clear()
}

interface ISpvStorage {
    fun getLastBlockHeader(): BlockHeader?
    fun saveBlockHeaders(blockHeaders: List<BlockHeader>)
    fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader>

    fun getAccountState(): AccountState?
    fun saveAccountSate(accountState: AccountState)

    fun saveTransactions(transactions: List<EthereumTransaction>)
    fun getTransactions(fromHash: ByteArray?, limit: Int?, contractAddress: ByteArray?): Single<List<EthereumTransaction>>

    fun clear()
}

interface IBlockchain {
    var listener: IBlockchainListener?

    val address: ByteArray

    fun start()
    fun stop()
    fun clear()

    val syncState: EthereumKit.SyncState
    fun getSyncStateErc20(contractAddress: ByteArray): EthereumKit.SyncState

    fun getLastBlockHeight(): Long?

    val balance: BigInteger?
    fun getBalanceErc20(contractAddress: ByteArray): BigInteger?

    fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>>
    fun getTransactionsErc20(contractAddress: ByteArray?, fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>>

    fun send(rawTransaction: RawTransaction): Single<EthereumTransaction>

    fun register(contractAddress: ByteArray)
    fun unregister(contractAddress: ByteArray)
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)

    fun onUpdateBalance(balance: BigInteger)
    fun onUpdateErc20Balance(balance: BigInteger, contractAddress: ByteArray)

    fun onUpdateSyncState(syncState: EthereumKit.SyncState)
    fun onUpdateErc20SyncState(syncState: EthereumKit.SyncState, contractAddress: ByteArray)

    fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>)
    fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: ByteArray)
}

interface IApiProvider {
    fun getLastBlockHeight(): Single<Long>
    fun getTransactionCount(address: ByteArray): Single<Long>

    fun getBalance(address: ByteArray): Single<BigInteger>
    fun getBalanceErc20(address: ByteArray, contractAddress: ByteArray): Single<BigInteger>

    fun getTransactions(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>>
    fun getTransactionsErc20(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>>

    fun send(signedTransaction: ByteArray): Single<Unit>
}
