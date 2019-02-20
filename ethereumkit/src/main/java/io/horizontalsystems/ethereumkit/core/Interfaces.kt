package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Flowable
import io.reactivex.Single
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.math.BigDecimal

interface IStorage {
    fun getLastBlockHeight(): Int?
    fun getGasPriceInWei(): Long?

    fun getBalance(address: String): BigDecimal?
    fun getLastTransactionBlockHeight(isErc20: Boolean): Int?
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>>

    fun saveLastBlockHeight(lastBlockHeight: Int)
    fun saveGasPriceInWei(gasPriceInWei: Long)
    fun saveBalance(balance: BigDecimal, address: String)
    fun saveTransactions(ethereumTransactions: List<EthereumTransaction>)

    fun clear()
}

interface IBlockchain {
    val ethereumAddress: String
    val gasPriceInWei: Long
    val gasLimitEthereum: Int
    val gasLimitErc20: Int

    var listener: IBlockchainListener?
    val blockchainSyncState: EthereumKit.SyncState

    fun start()
    fun stop()
    fun clear()

    fun syncState(contractAddress: String): EthereumKit.SyncState
    fun register(contractAddress: String, decimal: Int)
    fun unregister(contractAddress: String)

    fun send(toAddress: String, amount: BigDecimal, gasPriceInWei: Long?): Single<EthereumTransaction>
    fun sendErc20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPriceInWei: Long?): Single<EthereumTransaction>
}

interface IBlockchainListener {
    fun onUpdateLastBlockHeight(lastBlockHeight: Int)

    fun onUpdateState(syncState: EthereumKit.SyncState)
    fun onUpdateErc20State(syncState: EthereumKit.SyncState, contractAddress: String)

    fun onUpdateBalance(balance: BigDecimal)
    fun onUpdateErc20Balance(balance: BigDecimal, contractAddress: String)

    fun onUpdateTransactions(ethereumTransactions: List<EthereumTransaction>)
    fun onUpdateErc20Transactions(ethereumTransactions: List<EthereumTransaction>, contractAddress: String)
}

interface IWeb3jInfura {
    fun getGasPrice(): Flowable<BigDecimal>
    fun getLastBlockHeight(): Flowable<Int>
    fun getTransactionCount(address: String): EthGetTransactionCount

    fun getBalance(address: String): Flowable<BigDecimal>
    fun getBalanceErc20(address: String, contractAddress: String, decimal: Int): Flowable<BigDecimal>

    fun sendRawTransaction(rawTransaction: String): EthSendTransaction
    fun shutdown()
}

interface IApiProvider {
    fun getGasPriceInWei(): Single<Long>
    fun getLastBlockHeight(): Single<Int>
    fun getTransactionCount(address: String): Single<Int>

    fun getBalance(address: String): Single<BigDecimal>
    fun getBalanceErc20(address: String, contractAddress: String, decimal: Int): Single<BigDecimal>

    fun getTransactions(address: String, startBlock: Int): Single<List<EthereumTransaction>>
    fun getTransactionsErc20(address: String, startBlock: Int, decimals: HashMap<String, Int>): Single<List<EthereumTransaction>>

    fun send(fromAddress: String, toAddress: String, nonce: Int, amount: BigDecimal, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction>
    fun sendErc20(contractAddress: String, decimal: Int, fromAddress: String, toAddress: String, nonce: Int, amount: BigDecimal, gasPriceInWei: Long, gasLimit: Int): Single<EthereumTransaction>
}
