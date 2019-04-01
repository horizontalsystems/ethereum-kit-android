package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.horizontalsystems.ethereumkit.network.InfuraService
import io.reactivex.Single
import java.math.BigInteger

class ApiProvider(private val infuraService: InfuraService, private val etherscanService: EtherscanService) : IApiProvider {

    override fun getLastBlockHeight(): Single<Long> {
        return infuraService.getLastBlockHeight()
    }

    override fun getTransactionCount(address: ByteArray): Single<Long> {
        return infuraService.getTransactionCount(address)
    }

    override fun getBalance(address: ByteArray): Single<BigInteger> {
        return infuraService.getBalance(address)
    }

    override fun getBalanceErc20(address: ByteArray, contractAddress: ByteArray): Single<BigInteger> {
        return infuraService.getBalanceErc20(address, contractAddress)
    }

    override fun getTransactions(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response -> response.result.distinctBy { it.hash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
    }

    override fun getTransactionsErc20(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTokenTransactions(address, startBlock)
                .map { response -> response.result.distinctBy { it.hash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
    }

    override fun send(signedTransaction: ByteArray): Single<Unit> {
        return infuraService.send(signedTransaction)
    }

    companion object {
        fun getInstance(networkType: NetworkType, infuraApiKey: String, etherscanApiKey: String): ApiProvider {
            val infuraService = InfuraService(networkType, infuraApiKey)
            val etherscanService = EtherscanService(networkType, etherscanApiKey)

            return ApiProvider(infuraService, etherscanService)
        }
    }

}
