package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single

class TransactionsProvider(private val dataProvider: IDataProvider) : ITransactionsProvider {

    override fun getTransactions(contractAddress: ByteArray, address: ByteArray, from: Long, to: Long): Single<List<Transaction>> {
        return dataProvider.getTransactionLogs(contractAddress, address, from, to)
                .map { logs ->
                    logs.mapNotNull { getTransactionFromLog(it) }
                }
    }

    private fun getTransactionFromLog(log: EthereumLog): Transaction? {
        val value = log.data.hexStringToByteArray().toBigInteger()
        val from = log.topics[1].hexStringToByteArray().copyOfRange(12, 32)
        val to = log.topics[2].hexStringToByteArray().copyOfRange(12, 32)

        val transaction = Transaction(
                transactionHash = log.transactionHash.hexStringToByteArray(),
                interTransactionIndex = log.logIndex,
                transactionIndex = log.transactionIndex,
                from = from,
                to = to,
                value = value,
                timestamp = log.timestamp ?: System.currentTimeMillis() / 1000)

        transaction.logIndex = log.logIndex
        transaction.blockHash = log.blockHash.hexStringToByteArray()
        transaction.blockNumber = log.blockNumber

        return transaction
    }

}
