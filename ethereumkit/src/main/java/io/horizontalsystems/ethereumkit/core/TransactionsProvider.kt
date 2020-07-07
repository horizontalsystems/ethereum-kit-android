package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single
import java.math.BigInteger

class TransactionsProvider(
        private val etherscanService: EtherscanService,
        private val address: ByteArray
) : ITransactionsProvider {

    override val source: String
        get() = "etherscan.io"

    override fun getTransactions(startBlock: Long): Single<List<EthereumTransaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map { response ->
                    response.result.distinctBy { it["hash"] }.map { tx ->
                        val hash = tx["hash"]?.hexStringToByteArray() ?: ByteArray(0)
                        val nonce = tx["nonce"]?.toLongOrNull() ?: 0
                        val input = tx["input"]?.hexStringToByteArray() ?: ByteArray(0)
                        val from = tx["from"]?.hexStringToByteArray() ?: ByteArray(0)
                        val to = tx["to"]?.hexStringToByteArray() ?: ByteArray(0)
                        val value = tx["value"]?.toBigIntegerOrNull() ?: BigInteger.ZERO
                        val gasLimit = tx["gas"]?.toLongOrNull() ?: 0
                        val gasPrice = tx["gasPrice"]?.toLongOrNull() ?: 0
                        val timestamp = tx["timeStamp"]?.toLongOrNull() ?: 0

                        EthereumTransaction(hash, nonce, input, from, to, value, gasLimit, gasPrice, timestamp).apply {
                            this.blockHash = tx["blockHash"]?.hexStringToByteArray()
                            this.blockNumber = tx["blockNumber"]?.toLongOrNull()
                            this.gasUsed = tx["gasUsed"]?.toLongOrNull()
                            this.cumulativeGasUsed = tx["cumulativeGasUsed"]?.toLongOrNull()
                            this.iserror = tx["isError"]?.toIntOrNull()
                            this.transactionIndex = tx["transactionIndex"]?.toIntOrNull()
                            this.txReceiptStatus = tx["txreceipt_status"]?.toIntOrNull()
                        }
                    }
                }
    }

    override fun getInternalTransactions(startBlock: Long): Single<List<InternalTransaction>> {
        return etherscanService.getInternalTransactionList(address, startBlock)
                .map { response ->
                    response.result.map { internalTx ->
                        val hash = internalTx["hash"]?.hexStringToByteArray() ?: ByteArray(0)
                        val blockNumber = internalTx["blockNumber"]?.toLongOrNull() ?: 0
                        val from = internalTx["from"]?.hexStringToByteArray() ?: ByteArray(0)
                        val to = internalTx["to"]?.hexStringToByteArray() ?: ByteArray(0)
                        val value = internalTx["value"]?.toBigIntegerOrNull() ?: BigInteger.ZERO
                        val traceId = internalTx["traceId"]?.toIntOrNull() ?: 0

                        InternalTransaction(hash, blockNumber, from, to, value, traceId)
                    }
                }
    }

    companion object {
        fun getInstance(networkType: EthereumKit.NetworkType, etherscanApiKey: String, address: ByteArray): TransactionsProvider {
            val etherscanService = EtherscanService(networkType, etherscanApiKey)

            return TransactionsProvider(etherscanService, address)
        }
    }
}
