package io.horizontalsystems.ethereumkit.models

import java.math.BigInteger

data class TransactionData(
        val to: Address,
        val value: BigInteger,
        val input: ByteArray
)
