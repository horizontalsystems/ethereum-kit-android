package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class EtherscanTransactionsProvider(private val etherscanService: EtherscanService) : ITransactionsProvider {

    override fun getTransactions(contractAddress: Address, address: Address, startBlock: Long, endBlock: Long): Single<List<Transaction>> {
        return etherscanService.getTokenTransactions(contractAddress, address, startBlock)
                .map { response ->
                    val transactionIndexMap = mutableMapOf<String, Int>()

                    response.result.mapNotNull { tx ->
                        try {
                            val hash = tx.getValue("hash")
                            val transactionIndex = tx.getValue("transactionIndex").toInt()
                            val from = Address(tx.getValue("from"))
                            val to = Address(tx.getValue("to"))
                            val value = tx.getValue("value").toBigInteger()
                            val timestamp = tx.getValue("timeStamp").toLong()

                            val interTransactionIndex = transactionIndexMap[hash]?.plus(1) ?: 0
                            transactionIndexMap[hash] = interTransactionIndex

                            Transaction(hash.hexStringToByteArray(), interTransactionIndex, transactionIndex, from, to, value, timestamp)
                                    .apply {
                                        blockHash = tx["blockHash"]?.hexStringToByteArray()
                                        blockNumber = tx["blockNumber"]?.toLongOrNull()
                                    }
                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

}
