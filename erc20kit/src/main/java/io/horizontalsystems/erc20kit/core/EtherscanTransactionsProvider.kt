package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class EtherscanTransactionsProvider(private val etherscanService: EtherscanService) : ITransactionsProvider {

    override fun getTransactions(contractAddress: ByteArray, address: ByteArray, from: Long, to: Long): Single<List<Transaction>> {
        return etherscanService.getTokenTransactions(contractAddress, address, from)
                .map { response ->
                    val transactionIndexMap = mutableMapOf<String, Int>()

                    response.result.map { etherscanTx ->
                        val interTransactionIndex = transactionIndexMap[etherscanTx.hash]?.plus(1) ?: 0
                        transactionIndexMap[etherscanTx.hash] = interTransactionIndex

                        val transaction = Transaction(etherscanTx.hash.hexStringToByteArray(),
                                interTransactionIndex,
                                etherscanTx.transactionIndex.toIntOrNull(),
                                etherscanTx.from.hexStringToByteArray(),
                                etherscanTx.to.hexStringToByteArray(),
                                etherscanTx.value.toBigInteger(),
                                etherscanTx.timeStamp.toLong())

                        transaction.blockHash = etherscanTx.blockHash.hexStringToByteArray()
                        transaction.blockNumber = etherscanTx.blockNumber.toLongOrNull()

                        transaction
                    }
                }
    }

}
