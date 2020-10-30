package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class EstimateGasJsonRpc(
        @Transient val from: Address,
        @Transient val to: Address?,
        @Transient val amount: BigInteger?,
        @Transient val gasLimit: Long?,
        @Transient val gasPrice: Long?,
        @Transient val data: ByteArray?
) : LongJsonRpc(
        method = "eth_estimateGas",
        params = listOf(EstimateGasParams(from, to, amount, gasLimit, gasPrice, data))
) {

    data class EstimateGasParams(
            val from: Address,
            val to: Address?,
            @SerializedName("value")
            val amount: BigInteger?,
            @SerializedName("gas")
            val gasLimit: Long?,
            val gasPrice: Long?,
            val data: ByteArray?
    )
}
