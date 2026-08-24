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
import io.reactivex.Single
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

    override fun getTransactions(startBlock: Long): Single<List<ProviderTransaction>> =
        service.getTransactions(address.hex, startBlock).map { transactions ->
            transactions.mapNotNull { tx ->
                try {
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
                } catch (throwable: Throwable) {
                    null
                }
            }
        }

    override fun getInternalTransactions(startBlock: Long): Single<List<ProviderInternalTransaction>> =
        service.getInternalTransactions(address.hex, startBlock).map { internalTransactions ->
            internalTransactions.mapNotNull { mapInternalTransaction(it) }
        }

    override fun getInternalTransactionsAsync(hash: ByteArray): Single<List<ProviderInternalTransaction>> =
        service.getInternalTransactions(hash.toHexString()).map { internalTransactions ->
            internalTransactions.mapNotNull { mapInternalTransaction(it) }
        }

    override fun getTokenTransactions(startBlock: Long): Single<List<ProviderTokenTransaction>> =
        service.getTokenTransfers(address.hex, ERC20, startBlock).map { transfers ->
            transfers.mapNotNull { transfer ->
                try {
                    ProviderTokenTransaction(
                        blockNumber = transfer.blockNumber!!,
                        timestamp = parseTimestamp(transfer.timestamp),
                        hash = transfer.transactionHash!!.hexStringToByteArray(),
                        nonce = 0,
                        blockHash = transfer.blockHash?.hexStringToByteArray() ?: ByteArray(0),
                        from = Address(transfer.from!!.hash!!),
                        contractAddress = Address(transfer.token!!.addressHash!!),
                        to = Address(transfer.to!!.hash!!),
                        value = transfer.total!!.value!!.toBigInteger(),
                        tokenName = transfer.token.name ?: "",
                        tokenSymbol = transfer.token.symbol ?: "",
                        tokenDecimal = transfer.token.decimals?.toIntOrNull() ?: 0,
                        transactionIndex = 0,
                        gasLimit = 0,
                        gasPrice = 0,
                        gasUsed = 0,
                        cumulativeGasUsed = 0
                    )
                } catch (throwable: Throwable) {
                    null
                }
            }
        }

    override fun getEip721Transactions(startBlock: Long): Single<List<ProviderEip721Transaction>> =
        service.getTokenTransfers(address.hex, ERC721, startBlock).map { transfers ->
            transfers.mapNotNull { transfer ->
                try {
                    ProviderEip721Transaction(
                        blockNumber = transfer.blockNumber!!,
                        timestamp = parseTimestamp(transfer.timestamp),
                        hash = transfer.transactionHash!!.hexStringToByteArray(),
                        nonce = 0,
                        blockHash = transfer.blockHash?.hexStringToByteArray() ?: ByteArray(0),
                        transactionIndex = 0,
                        gasLimit = 0,
                        gasPrice = 0,
                        gasUsed = 0,
                        cumulativeGasUsed = 0,
                        contractAddress = Address(transfer.token!!.addressHash!!),
                        from = Address(transfer.from!!.hash!!),
                        to = Address(transfer.to!!.hash!!),
                        tokenId = transfer.total!!.tokenId!!.toBigInteger(),
                        tokenName = transfer.token.name ?: "",
                        tokenSymbol = transfer.token.symbol ?: "",
                        tokenDecimal = transfer.token.decimals?.toIntOrNull() ?: 0
                    )
                } catch (throwable: Throwable) {
                    null
                }
            }
        }

    override fun getEip1155Transactions(startBlock: Long): Single<List<ProviderEip1155Transaction>> =
        service.getTokenTransfers(address.hex, ERC1155, startBlock).map { transfers ->
            transfers.mapNotNull { transfer ->
                try {
                    ProviderEip1155Transaction(
                        blockNumber = transfer.blockNumber!!,
                        timestamp = parseTimestamp(transfer.timestamp),
                        hash = transfer.transactionHash!!.hexStringToByteArray(),
                        nonce = 0,
                        blockHash = transfer.blockHash?.hexStringToByteArray() ?: ByteArray(0),
                        transactionIndex = 0,
                        gasLimit = 0,
                        gasPrice = 0,
                        gasUsed = 0,
                        cumulativeGasUsed = 0,
                        contractAddress = Address(transfer.token!!.addressHash!!),
                        from = Address(transfer.from!!.hash!!),
                        to = Address(transfer.to!!.hash!!),
                        tokenId = transfer.total!!.tokenId!!.toBigInteger(),
                        tokenValue = transfer.total.value?.toIntOrNull() ?: 0,
                        tokenName = transfer.token.name ?: "",
                        tokenSymbol = transfer.token.symbol ?: ""
                    )
                } catch (throwable: Throwable) {
                    null
                }
            }
        }

    private fun mapInternalTransaction(internalTx: BlockscoutInternalTransaction): ProviderInternalTransaction? =
        try {
            ProviderInternalTransaction(
                hash = internalTx.transactionHash!!.hexStringToByteArray(),
                blockNumber = internalTx.blockNumber!!,
                timestamp = parseTimestamp(internalTx.timestamp),
                from = Address(internalTx.from!!.hash!!),
                to = Address(internalTx.to!!.hash!!),
                value = internalTx.value!!.toBigInteger(),
                traceId = (internalTx.index ?: 0).toString()
            )
        } catch (throwable: Throwable) {
            null
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
