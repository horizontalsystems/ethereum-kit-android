package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.BalanceOfMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.EthereumLog
import io.horizontalsystems.ethereumkit.models.TransactionStatus
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
import java.math.BigInteger

class DataProvider(private val ethereumKit: EthereumKit) : IDataProvider {

    private val transferEventTopic = CryptoUtils.sha3("Transfer(address,address,uint256)".toByteArray())

    override val lastBlockHeight: Long
        get() = ethereumKit.lastBlockHeight ?: 0

    override fun getTransactionLogs(contractAddress: Address, address: Address, from: Long,
                                    to: Long): Single<List<EthereumLog>> {
        val addressTopic = ByteArray(12) + address.raw

        val outgoingTopics = listOf(transferEventTopic, addressTopic)
        val incomingTopics = listOf(transferEventTopic, null, addressTopic)

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
                    logs.filter { log ->
                        log.topics.count() == 3
                                && log.topics[0] == transferEventTopic.toHexString()
                                && log.topics[1].hexStringToByteArray().count() == 32
                                && log.topics[2].hexStringToByteArray().count() == 32
                    }
                }
    }

    override fun getTransactionStatuses(transactionHashes: List<ByteArray>): Single<Map<ByteArray, TransactionStatus>> {

        val singles = transactionHashes.map { hash ->
            ethereumKit.transactionStatus(hash).flatMap { txStatus ->
                Single.just(Pair(hash, txStatus))
            }
        }

        return Single.zip(singles) { singleResults ->
            singleResults.map { it as? Pair<ByteArray, TransactionStatus> }
        }.flatMap { list ->
            val map = mutableMapOf<ByteArray, TransactionStatus>()
            list.forEach {
                if (it != null) {
                    map[it.first] = it.second
                }
            }
            Single.just(map)
        }
    }

    override fun getBalance(contractAddress: Address, address: Address): Single<BigInteger> {
        return ethereumKit.call(contractAddress, BalanceOfMethod(address).encodedABI())
                .map { it.toBigInteger() }
    }

    override fun send(contractAddress: Address, transactionInput: ByteArray, gasPrice: Long, gasLimit: Long): Single<ByteArray> {
        return ethereumKit.send(contractAddress, BigInteger.ZERO, transactionInput, gasPrice, gasLimit).map { tx ->
            tx.transaction.hash
        }
    }
}