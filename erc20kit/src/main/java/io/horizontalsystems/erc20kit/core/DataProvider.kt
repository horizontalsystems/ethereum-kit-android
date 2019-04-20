package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toBytesNoLeadZeroes
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.reactivex.Single
import java.math.BigInteger

class DataProvider(val ethereumKit: EthereumKit) : IDataProvider {

    override val lastBlockHeight: Long
        get() = ethereumKit.lastBlockHeight ?: 0

    override fun getTransactions(from: Long, to: Long, address: ByteArray): Single<List<Transaction>> {
        val addressTopic = ByteArray(12) { 0 } + address

        val outgoingTopics = listOf(Transaction.transferEventTopic, addressTopic)
        val incomingTopics = listOf(Transaction.transferEventTopic, null, addressTopic)

        val outgoingLogsRequest = ethereumKit.getLogs(null, outgoingTopics, from, to, true)
        val incomingLogsRequest = ethereumKit.getLogs(null, incomingTopics, from, to, true)

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

    override fun getStorageValue(contractAddress: ByteArray, position: Int, address: ByteArray, blockHeight: Long): Single<BigInteger> {
        val positionByteArray = position.toBytesNoLeadZeroes()

        var positionKeyData = ByteArray(12) + address
        positionKeyData += ByteArray(32 - positionByteArray.size) + positionByteArray

        val positionData = CryptoUtils.sha3(positionKeyData)

        return ethereumKit.getStorageAt(contractAddress, positionData, blockHeight).map {
            it.toBigInteger()
        }
    }

    override fun sendSingle(contractAddress: ByteArray, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray> {
        return ethereumKit.send(contractAddress, BigInteger.ZERO, transactionInput, gasPrice, gasLimit).map { txInfo ->
            txInfo.hash.hexStringToByteArray()
        }
    }
}