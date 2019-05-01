package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class TransactionsProvider(private val etherscanService: EtherscanService) : ITransactionsProvider {

    override fun getTransactions(address: ByteArray, startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response -> response.result.distinctBy { it.hash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
    }

    companion object {
        fun getInstance(networkType: EthereumKit.NetworkType, etherscanApiKey: String): TransactionsProvider {
            val etherscanService = EtherscanService(networkType, etherscanApiKey)

            return TransactionsProvider(etherscanService)
        }
    }
}
