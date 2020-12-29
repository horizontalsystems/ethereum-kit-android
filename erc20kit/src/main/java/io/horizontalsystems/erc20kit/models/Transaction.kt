package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import java.math.BigInteger

class Transaction(
        var transactionHash: ByteArray,
        var interTransactionIndex: Int = 0,
        var transactionIndex: Int? = null,
        val from: Address,
        val to: Address,
        val value: BigInteger,
        var timestamp: Long = System.currentTimeMillis() / 1000,
        var isError: Boolean = false,
        var type: TransactionType = TransactionType.TRANSFER,
        val fullTransaction: FullTransaction
)
