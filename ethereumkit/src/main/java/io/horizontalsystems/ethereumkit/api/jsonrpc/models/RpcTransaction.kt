package io.horizontalsystems.ethereumkit.api.jsonrpc.models

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class RpcTransaction(
        val hash: ByteArray,
        val nonce: Long,
        val blockHash: ByteArray?,
        val blockNumber: Long?,
        val transactionIndex: Long?,
        val from: Address,
        val to: Address?,
        val value: BigInteger,
        val gasPrice: Long,
        val maxFeePerGas: Long?,
        val maxPriorityFeePerGas: Long?,
        @SerializedName("gas")
        val gasLimit: Long,
        val input: ByteArray
)
