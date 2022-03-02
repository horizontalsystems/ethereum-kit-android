package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single

class EtherscanTransactionProvider(
        private val etherscanService: EtherscanService,
        private val address: Address
): ITransactionProvider {

    override fun getTransactions(startBlock: Long): Single<List<ProviderTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response ->
                    response.result.distinctBy { it["hash"] }.mapNotNull { tx ->
                        try {
                            val hash = tx.getValue("hash").hexStringToByteArray()
                            val nonce = tx.getValue("nonce").toLong()
                            val input = tx.getValue("input").hexStringToByteArray()
                            val from = Address(tx.getValue("from"))
                            val to = Address(tx.getValue("to"))
                            val value = tx.getValue("value").toBigInteger()
                            val gasLimit = tx.getValue("gas").toLong()
                            val gasPrice = tx.getValue("gasPrice").toLong()
                            val timestamp = tx.getValue("timeStamp").toLong()

                            ProviderTransaction(hash, nonce, input, from, to, value, gasLimit, gasPrice, timestamp)
                                    .apply {
                                        blockHash = tx["blockHash"]?.hexStringToByteArray()
                                        blockNumber = tx["blockNumber"]?.toLongOrNull()
                                        gasUsed = tx["gasUsed"]?.toLongOrNull()
                                        cumulativeGasUsed = tx["cumulativeGasUsed"]?.toLongOrNull()
                                        isError = tx["isError"]?.toIntOrNull()
                                        transactionIndex = tx["transactionIndex"]?.toIntOrNull()
                                        txReceiptStatus = tx["txreceipt_status"]?.toIntOrNull()
                                    }

                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getInternalTransactions(startBlock: Long): Single<List<InternalTransaction>> {
        return etherscanService.getInternalTransactionList(address, startBlock)
                .map { response ->
                    response.result.mapNotNull { internalTx ->
                        try {
                            val hash = internalTx.getValue("hash").hexStringToByteArray()
                            val blockNumber = internalTx.getValue("blockNumber").toLong()
                            val from = Address(internalTx.getValue("from"))
                            val to = Address(internalTx.getValue("to").hexStringToByteArray())
                            val value = internalTx.getValue("value").toBigInteger()
                            val traceId = internalTx.getValue("traceId")

                            InternalTransaction(hash, blockNumber, from, to, value, traceId)
                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getInternalTransactionsAsync(hash: ByteArray): Single<List<InternalTransaction>> {
        return etherscanService.getInternalTransactionsAsync(hash)
                .map { response ->
                    response.result.mapNotNull { internalTx ->
                        try {
                            val hash = hash
                            val blockNumber = internalTx.getValue("blockNumber").toLong()
                            val from = Address(internalTx.getValue("from"))
                            val to = Address(internalTx.getValue("to").hexStringToByteArray())
                            val value = internalTx.getValue("value").toBigInteger()
                            val traceId = ""

                            InternalTransaction(hash, blockNumber, from, to, value, traceId)
                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

    override fun getTokenTransactions(startBlock: Long): Single<List<ProviderTokenTransaction>> {
        return etherscanService.getTokenTransactions(address, startBlock)
                .map { response ->
                    response.result.mapNotNull { tx ->
                        try {
                            ProviderTokenTransaction(
                                    hash = tx.getValue("hash").hexStringToByteArray(),
                                    blockNumber = tx["blockNumber"]?.toLongOrNull())

                        } catch (throwable: Throwable) {
                            null
                        }
                    }
                }
    }

}
