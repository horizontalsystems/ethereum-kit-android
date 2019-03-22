package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.FeePriority
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Single

interface IStorage {
    fun getLastBlockHeight(): Long?
    fun getBalance(address: String): String?
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>>
    fun clear()
}

interface IApiStorage : IStorage {
    fun getGasPriceInWei(): GasPrice?
    fun getLastTransactionBlockHeight(isErc20: Boolean): Int?
    fun saveLastBlockHeight(lastBlockHeight: Int)
    fun saveGasPriceInWei(gasPriceInWei: GasPrice)
    fun saveBalance(balance: String, address: String)
    fun saveTransactions(ethereumTransactions: List<EthereumTransaction>)
}

interface ISpvStorage : IStorage {
    fun getLastBlockHeader(): BlockHeader?
    fun saveBlockHeaders(blockHeaders: List<BlockHeader>)
    fun getBlockHeadersReversed(fromBlockHeight: Long, limit: Int): List<BlockHeader>
}

interface IBlockchain {
    val ethereumAddress: String
    val gasPriceData: GasPrice
    val gasLimitEthereum: Int
    val gasLimitErc20: Int

    var listener: IBlockchainListener?
    val blockchainSyncState: EthereumKit.SyncState

    fun start()
    fun stop()
    fun clear()

    fun gasPriceInWei(feePriority: FeePriority): Long
    fun syncState(contractAddress: String): EthereumKit.SyncState
    fun register(contractAddress: String)
    fun unregister(contractAddress: String)

    fun send(toAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction>
    fun sendErc20(toAddress: String, contractAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction>
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Long)

    fun onUpdateState(syncState: EthereumKit.SyncState)
    fun onUpdateErc20State(syncState: EthereumKit.SyncState, contractAddress: String)

    fun onUpdateBalance(balance: String)
    fun onUpdateErc20Balance(balance: String, contractAddress: String)

    fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>)
    fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: String)
}

interface IApiProvider {
    fun getGasPriceInWei(): Single<Long>
    fun getLastBlockHeight(): Single<Int>
    fun getTransactionCount(address: String): Single<Int>

    fun getBalance(address: String): Single<String>
    fun getBalanceErc20(address: String, contractAddress: String): Single<String>

    fun getTransactions(address: String, startBlock: Int): Single<List<EthereumTransaction>>
    fun getTransactionsErc20(address: String, startBlock: Int): Single<List<EthereumTransaction>>

    fun send(fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction>
    fun sendErc20(contractAddress: String, fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction>
}
