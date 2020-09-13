package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.Erc20SmartContract
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.Single
import io.reactivex.functions.BiFunction

class EtherscanTransactionsProvider(private val etherscanService: EtherscanService) : ITransactionsProvider {

    private val erc20SmartContract = Erc20SmartContract()

    override fun getTransactions(contractAddress: Address, address: Address, startBlock: Long, endBlock: Long): Single<List<Transaction>> {
        val fromInput = getEthereumTransactions(address, contractAddress, startBlock)
                .map {
                    it.map { ethTx ->
                        erc20SmartContract.getPotentialErc20TransactionsFromEthTransaction(ethTx)
                    }.flatten()
                }

        val transfers = etherscanService.getTokenTransactions(contractAddress, address, startBlock)
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

        return Single.zip(fromInput, transfers, BiFunction<List<Transaction>, List<Transaction>, List<Transaction>> { t1, t2 ->
            (t1 + t2).sortedByDescending { it.timestamp }
        })
    }

    private fun getEthereumTransactions(address: Address, contractAddress: Address, startBlock: Long): Single<List<io.horizontalsystems.ethereumkit.models.Transaction>> {
        return etherscanService.getTransactionList(address, startBlock)
                .map {
                    it.result.mapNotNull { tx ->
                        try {
                            val toAddressHex = tx.getValue("to")
                            if (toAddressHex != contractAddress.hex) return@mapNotNull null

                            val hash = tx.getValue("hash").hexStringToByteArray()
                            val nonce = tx.getValue("nonce").toLong()
                            val input = tx.getValue("input").hexStringToByteArray()
                            val from = Address(tx.getValue("from"))
                            val to = Address(toAddressHex)
                            val value = tx.getValue("value").toBigInteger()
                            val gasLimit = tx.getValue("gas").toLong()
                            val gasPrice = tx.getValue("gasPrice").toLong()
                            val timestamp = tx.getValue("timeStamp").toLong()

                            io.horizontalsystems.ethereumkit.models.Transaction(hash, nonce, input, from, to, value, gasLimit, gasPrice, timestamp)
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

}
