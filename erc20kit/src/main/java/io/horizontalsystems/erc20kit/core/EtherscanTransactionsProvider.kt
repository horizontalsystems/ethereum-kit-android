package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single
import java.math.BigInteger

class EtherscanTransactionsProvider(private val etherscanService: EtherscanService) : ITransactionsProvider {

    override fun getTransactions(contractAddress: ByteArray, address: ByteArray, startBlock: Long, endBlock: Long): Single<List<Transaction>> {
        return etherscanService.getTokenTransactions(contractAddress, address, startBlock)
                .map { response ->
                    val transactionIndexMap = mutableMapOf<String, Int>()

                    response.result.map { tx ->
                        val hash = tx["hash"] ?: ""
                        val transactionIndex = tx["transactionIndex"]?.toIntOrNull()
                        val from = tx["from"]?.hexStringToByteArray() ?: ByteArray(0)
                        val to = tx["to"]?.hexStringToByteArray() ?: ByteArray(0)
                        val value = tx["value"]?.toBigIntegerOrNull() ?: BigInteger.ZERO
                        val timestamp = tx["timeStamp"]?.toLongOrNull() ?: 0

                        val interTransactionIndex = transactionIndexMap[hash]?.plus(1) ?: 0
                        transactionIndexMap[hash] = interTransactionIndex

                        Transaction(hash.hexStringToByteArray(),
                                interTransactionIndex,
                                transactionIndex,
                                from,
                                to,
                                value,
                                timestamp).apply {
                            this.blockHash = tx["blockHash"]?.hexStringToByteArray()
                            this.blockNumber = tx["blockNumber"]?.toLongOrNull()
                        }
                    }
                }
    }

}
