package io.horizontalsystems.ethereumkit.models

import java.math.BigInteger

data class ProviderTransaction(
    var blockNumber: Long,
    val timestamp: Long,
    val hash: ByteArray,
    val nonce: Long,
    var blockHash: ByteArray? = null,
    var transactionIndex: Int,
    val from: Address,
    val to: Address?,
    val value: BigInteger,
    val gasLimit: Long,
    val gasPrice: Long,
    var isError: Int? = null,
    var txReceiptStatus: Int? = null,
    val input: ByteArray,
    var cumulativeGasUsed: Long? = null,
    var gasUsed: Long? = null
)

data class ProviderTokenTransaction(
    var blockNumber: Long,
    val timestamp: Long,
    val hash: ByteArray,
    val nonce: Long,
    var blockHash: ByteArray,
    val from: Address,
    val contractAddress: Address,
    val to: Address,
    val value: BigInteger,

    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: Int,

    var transactionIndex: Int,
    val gasLimit: Long,
    val gasPrice: Long,
    var gasUsed: Long,
    var cumulativeGasUsed: Long
)

data class ProviderInternalTransaction(
    val hash: ByteArray,
    val blockNumber: Long,
    val timestamp: Long,
    val from: Address,
    val to: Address,
    val value: BigInteger,
    val traceId: String
) {

    fun internalTransaction() = InternalTransaction(hash, blockNumber, from, to, value)

}

data class ProviderEip721Transaction(
    val blockNumber: Long,
    val timestamp: Long,
    val hash: ByteArray,
    val nonce: Long,
    val blockHash: ByteArray,
    val transactionIndex: Int,
    val gasLimit: Long,
    val gasPrice: Long,
    val gasUsed: Long,
    val cumulativeGasUsed: Long,

    val contractAddress: Address,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val tokenName: String,
    val tokenSymbol: String,
    val tokenDecimal: Int
)

data class ProviderEip1155Transaction(
    val blockNumber: Long,
    val timestamp: Long,
    val hash: ByteArray,
    val nonce: Long,
    val blockHash: ByteArray,
    val transactionIndex: Int,
    val gasLimit: Long,
    val gasPrice: Long,
    val gasUsed: Long,
    val cumulativeGasUsed: Long,

    val contractAddress: Address,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val tokenValue: Int,
    val tokenName: String,
    val tokenSymbol: String
)