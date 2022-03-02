package io.horizontalsystems.ethereumkit.models

import java.math.BigInteger

data class ProviderTransaction(
        val hash: ByteArray,
        val nonce: Long,
        val input: ByteArray,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        val gasLimit: Long,
        val gasPrice: Long,
        val timestamp: Long,
        var blockHash: ByteArray? = null,
        var blockNumber: Long? = null,
        var gasUsed: Long? = null,
        var cumulativeGasUsed: Long? = null,
        var isError: Int? = null,
        var transactionIndex: Int? = null,
        var txReceiptStatus: Int? = null
)

data class ProviderTokenTransaction(
        val hash: ByteArray,
        val blockNumber: Long?,
)
