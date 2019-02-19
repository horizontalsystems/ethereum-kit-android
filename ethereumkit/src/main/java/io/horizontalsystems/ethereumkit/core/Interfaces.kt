package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionRoom
import io.reactivex.Flowable
import io.reactivex.Single
import org.web3j.protocol.core.methods.response.EthGetTransactionCount
import org.web3j.protocol.core.methods.response.EthSendTransaction
import java.math.BigDecimal

interface IStorage{
    fun getLastBlockHeight(): Int?
    fun getGasPrice(): BigDecimal?

    fun getBalance(address: String): BigDecimal?
    fun getLastTransactionBlockHeight(isErc20: Boolean): Int?
    fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<TransactionRoom>>

    fun saveLastBlockHeight(lastBlockHeight: Int)
    fun saveGasPrice(gasPrice: BigDecimal)
    fun saveBalance(balance: BigDecimal, address: String)
    fun saveTransactions(transactions: List<TransactionRoom>)

    fun clear()
}

interface IBlockchain{
    val ethereumGasLimit: Int
    val erc20GasLimit: Int

    var listener: IBlockchainListener?

    val ethereumAddress: String
    val gasPrice: BigDecimal

    fun start()
    fun stop()
    fun clear()

    fun register(contractAddress: String, decimal: Int)
    fun unregister(contractAddress: String)

    fun send(toAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)?, onError: (() -> Unit)?)
    fun sendErc20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPrice: BigDecimal?, onSuccess: (() -> Unit)?, onError: (() -> Unit)?)
}

interface IBlockchainListener{
    fun onUpdateLastBlockHeight(lastBlockHeight: Int)

    fun onUpdateState(syncState: EthereumKit.SyncState)
    fun onUpdateErc20State(syncState: EthereumKit.SyncState, contractAddress: String)

    fun onUpdateBalance(balance: BigDecimal)
    fun onUpdateErc20Balance(balance: BigDecimal, contractAddress: String)

    fun onUpdateTransactions(transactions: List<TransactionRoom>)
    fun onUpdateErc20Transactions(transactions: List<TransactionRoom>, contractAddress: String)
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
