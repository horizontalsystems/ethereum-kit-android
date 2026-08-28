package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.network.EtherscanService

class EtherscanTransactionProvider(
    private val etherscanService: EtherscanService,
    private val address: Address
) : ITransactionProvider {

    override suspend fun getTransactions(startBlock: Long): List<ProviderTransaction> {
        val response = etherscanService.getTransactionList(address, startBlock)
        return response.result.distinctBy { it["hash"] }.mapNotNull { tx ->
            try {
                val blockNumber = tx.getValue("blockNumber").toLong()
                val timestamp = tx.getValue("timeStamp").toLong()
                val hash = tx.getValue("hash").hexStringToByteArray()
                val nonce = tx.getValue("nonce").toLong()
                val blockHash = tx["blockHash"]?.hexStringToByteArray()
                val transactionIndex = tx.getValue("transactionIndex").toInt()
                val from = Address(tx.getValue("from"))
                val to = getAddressOrNull(tx["to"])
                val value = tx.getValue("value").toBigInteger()
                val gasLimit = tx.getValue("gas").toLong()
                val gasPrice = tx.getValue("gasPrice").toLong()
                val isError = tx["isError"]?.toIntOrNull()
                val txReceiptStatus = tx["txreceipt_status"]?.toIntOrNull()
                val input = tx.getValue("input").hexStringToByteArray()
                val cumulativeGasUsed = tx["cumulativeGasUsed"]?.toLongOrNull()
                val gasUsed = tx["gasUsed"]?.toLongOrNull()

                ProviderTransaction(
                    blockNumber, timestamp, hash, nonce, blockHash, transactionIndex, from, to, value, gasLimit, gasPrice,
                    isError, txReceiptStatus, input, cumulativeGasUsed, gasUsed
                )

            } catch (throwable: Throwable) {
                null
            }
        }
    }

    override suspend fun getInternalTransactions(startBlock: Long): List<ProviderInternalTransaction> {
        val response = etherscanService.getInternalTransactionList(address, startBlock)
        return response.result.mapNotNull { internalTx ->
            try {
                val hash = internalTx.getValue("hash").hexStringToByteArray()
                val blockNumber = internalTx.getValue("blockNumber").toLong()
                val timestamp = internalTx.getValue("timeStamp").toLong()
                val from = Address(internalTx.getValue("from"))
                val to = Address(internalTx.getValue("to"))
                val value = internalTx.getValue("value").toBigInteger()
                val traceId = internalTx.getValue("traceId")

                ProviderInternalTransaction(hash, blockNumber, timestamp, from, to, value, traceId)
            } catch (throwable: Throwable) {
                null
            }
        }
    }

    override suspend fun getInternalTransactionsAsync(hash: ByteArray): List<ProviderInternalTransaction> {
        val response = etherscanService.getInternalTransactionsAsync(hash)
        return response.result.mapNotNull { internalTx ->
            try {
                val blockNumber = internalTx.getValue("blockNumber").toLong()
                val timestamp = internalTx.getValue("timeStamp").toLong()
                val from = Address(internalTx.getValue("from"))
                val to = Address(internalTx.getValue("to"))
                val value = internalTx.getValue("value").toBigInteger()
                val traceId = internalTx.getValue("traceId")

                ProviderInternalTransaction(hash, blockNumber, timestamp, from, to, value, traceId)
            } catch (throwable: Throwable) {
                null
            }
        }
    }

    override suspend fun getTokenTransactions(startBlock: Long): List<ProviderTokenTransaction> {
        val response = etherscanService.getTokenTransactions(address, startBlock)
        return response.result.mapNotNull { tx ->
            try {
                val blockNumber = tx.getValue("blockNumber").toLong()
                val timestamp = tx.getValue("timeStamp").toLong()
                val hash = tx.getValue("hash").hexStringToByteArray()
                val nonce = tx.getValue("nonce").toLong()
                val blockHash = tx.getValue("blockHash").hexStringToByteArray()
                val from = Address(tx.getValue("from"))
                val contractAddress = Address(tx.getValue("contractAddress"))
                val to = Address(tx.getValue("to"))
                val value = tx.getValue("value").toBigInteger()
                val tokenName = tx.getValue("tokenName")
                val tokenSymbol = tx.getValue("tokenSymbol")
                val tokenDecimal = tx.getValue("tokenDecimal").toInt()
                val transactionIndex = tx.getValue("transactionIndex").toInt()
                val gasLimit = tx.getValue("gas").toLong()
                val gasPrice = tx.getValue("gasPrice").toLong()
                val gasUsed = tx.getValue("gasUsed").toLong()
                val cumulativeGasUsed = tx.getValue("cumulativeGasUsed").toLong()

                ProviderTokenTransaction(
                    blockNumber, timestamp, hash, nonce, blockHash, from, contractAddress, to, value, tokenName, tokenSymbol, tokenDecimal,
                    transactionIndex, gasLimit, gasPrice, gasUsed, cumulativeGasUsed
                )

            } catch (throwable: Throwable) {
                null
            }
        }
    }

    override suspend fun getEip721Transactions(startBlock: Long): List<ProviderEip721Transaction> {
        val response = etherscanService.getEip721Transactions(address, startBlock)
        return response.result.mapNotNull { tx ->
            try {
                val blockNumber = tx.getValue("blockNumber").toLong()
                val timestamp = tx.getValue("timeStamp").toLong()
                val hash = tx.getValue("hash").hexStringToByteArray()
                val nonce = tx.getValue("nonce").toLong()
                val blockHash = tx.getValue("blockHash").hexStringToByteArray()
                val from = Address(tx.getValue("from"))
                val contractAddress = Address(tx.getValue("contractAddress"))
                val to = Address(tx.getValue("to"))
                val tokenId = tx.getValue("tokenID").toBigInteger()
                val tokenName = tx.getValue("tokenName")
                val tokenSymbol = tx.getValue("tokenSymbol")
                val tokenDecimal = tx.getValue("tokenDecimal").toInt()
                val transactionIndex = tx.getValue("transactionIndex").toInt()
                val gasLimit = tx.getValue("gas").toLong()
                val gasPrice = tx.getValue("gasPrice").toLong()
                val gasUsed = tx.getValue("gasUsed").toLong()
                val cumulativeGasUsed = tx.getValue("cumulativeGasUsed").toLong()

                ProviderEip721Transaction(
                    blockNumber = blockNumber,
                    timestamp = timestamp,
                    hash = hash,
                    nonce = nonce,
                    blockHash = blockHash,
                    transactionIndex = transactionIndex,
                    gasLimit = gasLimit,
                    gasPrice = gasPrice,
                    gasUsed = gasUsed,
                    cumulativeGasUsed = cumulativeGasUsed,
                    contractAddress = contractAddress,
                    from = from,
                    to = to,
                    tokenId = tokenId,
                    tokenName = tokenName,
                    tokenSymbol = tokenSymbol,
                    tokenDecimal = tokenDecimal
                )
            } catch (throwable: Throwable) {
                null
            }
        }
    }

    override suspend fun getEip1155Transactions(startBlock: Long): List<ProviderEip1155Transaction> {
        val response = etherscanService.getEip1155Transactions(address, startBlock)
        return response.result.mapNotNull { tx ->
            try {
                val blockNumber = tx.getValue("blockNumber").toLong()
                val timestamp = tx.getValue("timeStamp").toLong()
                val hash = tx.getValue("hash").hexStringToByteArray()
                val nonce = tx.getValue("nonce").toLong()
                val blockHash = tx.getValue("blockHash").hexStringToByteArray()
                val from = Address(tx.getValue("from"))
                val contractAddress = Address(tx.getValue("contractAddress"))
                val to = Address(tx.getValue("to"))

                val tokenId = tx.getValue("tokenID").toBigInteger()
                val tokenValue = tx.getValue("tokenValue").toInt()
                val tokenName = tx.getValue("tokenName")
                val tokenSymbol = tx.getValue("tokenSymbol")
                val transactionIndex = tx.getValue("transactionIndex").toInt()
                val gasLimit = tx.getValue("gas").toLong()
                val gasPrice = tx.getValue("gasPrice").toLong()
                val gasUsed = tx.getValue("gasUsed").toLong()
                val cumulativeGasUsed = tx.getValue("cumulativeGasUsed").toLong()

                ProviderEip1155Transaction(
                    blockNumber = blockNumber,
                    timestamp = timestamp,
                    hash = hash,
                    nonce = nonce,
                    blockHash = blockHash,
                    transactionIndex = transactionIndex,
                    gasLimit = gasLimit,
                    gasPrice = gasPrice,
                    gasUsed = gasUsed,
                    cumulativeGasUsed = cumulativeGasUsed,
                    contractAddress = contractAddress,
                    from = from,
                    to = to,
                    tokenId = tokenId,
                    tokenValue = tokenValue,
                    tokenName = tokenName,
                    tokenSymbol = tokenSymbol
                )
            } catch (throwable: Throwable) {
                null
            }
        }
    }

    private fun getAddressOrNull(addressString: String?): Address? =
        if (!addressString.isNullOrEmpty()) Address(addressString) else null

}
