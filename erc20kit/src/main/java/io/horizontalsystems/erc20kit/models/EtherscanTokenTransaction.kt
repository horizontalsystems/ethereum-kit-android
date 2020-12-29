package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

data class EtherscanTokenTransaction(
        val hash: ByteArray,
        val transactionIndex: Int,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        val timestamp: Long,
        val blockHash: ByteArray?,
        val blockNumber: Long?,
)
