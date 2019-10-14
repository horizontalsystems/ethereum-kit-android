package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class TransactionsProvider(
        private val etherscanService: EtherscanService,
        private val address: ByteArray
) : ITransactionsProvider {

    override val source: String
        get() = "etherscan.io"

    override fun getTransactions(startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response -> response.result.distinctBy { it.hash }.map { etherscanTransaction -> EthereumTransaction(etherscanTransaction) } }
    }

    companion object {
        fun getInstance(networkType: EthereumKit.NetworkType, etherscanApiKey: String, address: ByteArray): TransactionsProvider {
            val etherscanService = EtherscanService(networkType, etherscanApiKey)

            return TransactionsProvider(etherscanService, address)
        }
    }
}
