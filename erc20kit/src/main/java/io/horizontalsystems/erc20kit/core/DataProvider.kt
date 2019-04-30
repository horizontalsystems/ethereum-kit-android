package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.network.ERC20
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
import java.math.BigInteger

class DataProvider(private val ethereumKit: EthereumKit) : IDataProvider {

    override val lastBlockHeight: Long
        get() = ethereumKit.lastBlockHeight ?: 0

    override fun getTransactions(contractAddress: ByteArray, address: ByteArray, from: Long, to: Long): Single<List<Transaction>> {
        val addressTopic = ByteArray(12) { 0 } + address

        val outgoingTopics = listOf(Transaction.transferEventTopic, addressTopic)
        val incomingTopics = listOf(Transaction.transferEventTopic, null, addressTopic)

        val outgoingLogsRequest = ethereumKit.getLogs(contractAddress, outgoingTopics, from, to, true)
        val incomingLogsRequest = ethereumKit.getLogs(contractAddress, incomingTopics, from, to, true)

        return Single.merge(outgoingLogsRequest, incomingLogsRequest).toList()
                .map { results ->
                    val logs = mutableListOf<EthereumLog>()

                    for (result in results) {
                        for (log in result) {
                            if (!logs.contains(log)) {
                                logs.add(log)
                            }
                        }
                    }
                    logs.mapNotNull {
                        Transaction.createFromLog(it)
                    }
                }
    }

    override fun getBalance(contractAddress: ByteArray, address: ByteArray): Single<BigInteger> {
        val balanceOfData = ERC20.encodeFunctionBalanceOf(address)

        return ethereumKit.call(contractAddress, balanceOfData)
                .map { it.toBigInteger() }
    }

    override fun sendSingle(contractAddress: ByteArray, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray> {
        return ethereumKit.send(contractAddress, BigInteger.ZERO, transactionInput, gasPrice, gasLimit).map { txInfo ->
            txInfo.hash.hexStringToByteArray()
        }
    }
}