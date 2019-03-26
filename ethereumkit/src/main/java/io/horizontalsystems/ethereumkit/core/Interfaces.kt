package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.AccountState
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.reactivex.Single


interface IApiStorage {
    fun getLastBlockHeight(): Long?
    fun getBalance(address: String): String?
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>>

    fun getLastTransactionBlockHeight(isErc20: Boolean): Int?
    fun saveLastBlockHeight(lastBlockHeight: Int)
    fun saveBalance(balance: String, address: String)
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
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>>

    fun clear()
}

interface IBlockchain {
    var listener: IBlockchainListener?

    val address: String

    val balance: String?
    fun getBalanceErc20(contractAddress: String): String?

    val syncState: EthereumKit.SyncState
    fun syncStateErc20(contractAddress: String): EthereumKit.SyncState

    fun getLastBlockHeight(): Long?
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>>


    fun start()
    fun stop()
    fun clear()

    fun register(contractAddress: String)
    fun unregister(contractAddress: String)

    fun send(toAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction>
    fun sendErc20(toAddress: String, contractAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction>
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

    fun send(fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Long): Single<EthereumTransaction>
    fun sendErc20(contractAddress: String, fromAddress: String, toAddress: String, nonce: Int, amount: String, gasPriceInWei: Long, gasLimit: Long): Single<EthereumTransaction>
}
