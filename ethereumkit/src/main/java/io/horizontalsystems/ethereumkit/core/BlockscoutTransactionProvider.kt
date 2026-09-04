package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.ProviderEip1155Transaction
import io.horizontalsystems.ethereumkit.models.ProviderEip721Transaction
import io.horizontalsystems.ethereumkit.models.ProviderInternalTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.ProviderTransaction
import io.horizontalsystems.ethereumkit.network.BlockscoutInternalTransaction
import io.horizontalsystems.ethereumkit.network.BlockscoutService
import io.horizontalsystems.ethereumkit.network.BlockscoutTokenTransfer
import java.math.BigInteger
import java.time.Instant

/**
 * [ITransactionProvider] backed by a Blockscout instance's `/api/v2` REST API.
 *
 * The v2 endpoints do not carry every field the legacy Etherscan API does. Missing fields that are
 * not used when building [io.horizontalsystems.ethereumkit.models.Transaction] records or decorating
 * them (e.g. per-token-transfer gas figures, nonce, transaction index) are defaulted to 0; token
 * transfer fees are taken from the parent transaction record, not the transfer itself.
 */
class BlockscoutTransactionProvider(
    private val service: BlockscoutService,
    private val address: Address,
) : ITransactionProvider {

    override suspend fun getTransactions(startBlock: Long): List<ProviderTransaction> =
        service.getTransactions(address.hex, startBlock).mapValid { tx ->
            val success = tx.status == "ok"
            ProviderTransaction(
                blockNumber = tx.blockNumber!!,
                timestamp = parseTimestamp(tx.timestamp),
                hash = tx.hash!!.hexStringToByteArray(),
                nonce = tx.nonce ?: 0,
                blockHash = null,
                transactionIndex = tx.position ?: 0,
                from = Address(tx.from!!.hash!!),
                to = tx.to?.hash?.let { Address(it) },
                value = tx.value!!.toBigInteger(),
                gasLimit = tx.gasLimit?.toLong() ?: 0,
                gasPrice = tx.gasPrice?.toLong() ?: 0,
                isError = if (success) 0 else 1,
                txReceiptStatus = if (success) 1 else 0,
                input = tx.rawInput?.hexStringToByteArray() ?: ByteArray(0),
                cumulativeGasUsed = null,
                gasUsed = tx.gasUsed?.toLongOrNull()
            )
        }

    override suspend fun getInternalTransactions(startBlock: Long): List<ProviderInternalTransaction> =
        service.getInternalTransactions(address.hex, startBlock).mapValid(::mapInternalTransaction)

    override suspend fun getInternalTransactionsAsync(hash: ByteArray): List<ProviderInternalTransaction> =
        service.getInternalTransactions(hash.toHexString()).mapValid(::mapInternalTransaction)

    override suspend fun getTokenTransactions(startBlock: Long): List<ProviderTokenTransaction> =
        service.getTokenTransfers(address.hex, ERC20, startBlock).mapValid { transfer ->
            val fields = TransferFields(transfer)
            ProviderTokenTransaction(
                blockNumber = fields.blockNumber,
                timestamp = fields.timestamp,
                hash = fields.hash,
                nonce = 0,
                blockHash = fields.blockHash,
                from = fields.from,
                contractAddress = fields.contractAddress,
                to = fields.to,
                value = transfer.total!!.value!!.toBigInteger(),
                tokenName = fields.tokenName,
                tokenSymbol = fields.tokenSymbol,
                tokenDecimal = fields.tokenDecimal,
                transactionIndex = 0,
                gasLimit = 0,
                gasPrice = 0,
                gasUsed = 0,
                cumulativeGasUsed = 0
            )
        }

    override suspend fun getEip721Transactions(startBlock: Long): List<ProviderEip721Transaction> =
        service.getTokenTransfers(address.hex, ERC721, startBlock).mapValid { transfer ->
            val fields = TransferFields(transfer)
            ProviderEip721Transaction(
                blockNumber = fields.blockNumber,
                timestamp = fields.timestamp,
                hash = fields.hash,
                nonce = 0,
                blockHash = fields.blockHash,
                transactionIndex = 0,
                gasLimit = 0,
                gasPrice = 0,
                gasUsed = 0,
                cumulativeGasUsed = 0,
                contractAddress = fields.contractAddress,
                from = fields.from,
                to = fields.to,
                tokenId = fields.tokenId,
                tokenName = fields.tokenName,
                tokenSymbol = fields.tokenSymbol,
                tokenDecimal = fields.tokenDecimal
            )
        }

    override suspend fun getEip1155Transactions(startBlock: Long): List<ProviderEip1155Transaction> =
        service.getTokenTransfers(address.hex, ERC1155, startBlock).mapValid { transfer ->
            val fields = TransferFields(transfer)
            ProviderEip1155Transaction(
                blockNumber = fields.blockNumber,
                timestamp = fields.timestamp,
                hash = fields.hash,
                nonce = 0,
                blockHash = fields.blockHash,
                transactionIndex = 0,
                gasLimit = 0,
                gasPrice = 0,
                gasUsed = 0,
                cumulativeGasUsed = 0,
                contractAddress = fields.contractAddress,
                from = fields.from,
                to = fields.to,
                tokenId = fields.tokenId,
                tokenValue = transfer.total?.value?.toIntOrNull() ?: 0,
                tokenName = fields.tokenName,
                tokenSymbol = fields.tokenSymbol
            )
        }

    private fun mapInternalTransaction(internalTx: BlockscoutInternalTransaction) =
        ProviderInternalTransaction(
            hash = internalTx.transactionHash!!.hexStringToByteArray(),
            blockNumber = internalTx.blockNumber!!,
            timestamp = parseTimestamp(internalTx.timestamp),
            from = Address(internalTx.from!!.hash!!),
            to = Address(internalTx.to!!.hash!!),
            value = internalTx.value!!.toBigInteger(),
            traceId = (internalTx.index ?: 0).toString()
        )

    /**
     * The fields shared by every token-transfer record (ERC-20/721/1155), parsed once. Any
     * required field that is missing throws, which [mapValid] turns into skipping the record.
     */
    private inner class TransferFields(private val transfer: BlockscoutTokenTransfer) {
        val blockNumber: Long = transfer.blockNumber!!
        val timestamp: Long = parseTimestamp(transfer.timestamp)
        val hash: ByteArray = transfer.transactionHash!!.hexStringToByteArray()
        val blockHash: ByteArray = transfer.blockHash?.hexStringToByteArray() ?: ByteArray(0)
        val from: Address = Address(transfer.from!!.hash!!)
        val to: Address = Address(transfer.to!!.hash!!)
        private val token = transfer.token!!
        val contractAddress: Address = Address(token.addressHash!!)
        val tokenName: String = token.name ?: ""
        val tokenSymbol: String = token.symbol ?: ""
        val tokenDecimal: Int = token.decimals?.toIntOrNull() ?: 0
        // Only present on NFT transfers, so resolved lazily rather than in the constructor.
        val tokenId: BigInteger get() = transfer.total!!.tokenId!!.toBigInteger()
    }

    /** Maps each record, silently dropping any whose required fields are missing or malformed. */
    private inline fun <T, R : Any> List<T>.mapValid(transform: (T) -> R): List<R> =
        mapNotNull { item ->
            try {
                transform(item)
            } catch (throwable: Throwable) {
                null
            }
        }

    private fun parseTimestamp(timestamp: String?): Long =
        timestamp?.let {
            try {
                Instant.parse(it).epochSecond
            } catch (throwable: Throwable) {
                null
            }
        } ?: 0

    companion object {
        private const val ERC20 = "ERC-20"
        private const val ERC721 = "ERC-721"
        private const val ERC1155 = "ERC-1155"
    }
}
