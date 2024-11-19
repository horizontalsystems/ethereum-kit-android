package io.horizontalsystems.ethereumkit.api.jsonrpc

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.GasPrice
import java.math.BigInteger

class EstimateGasJsonRpc(
        @Transient val from: Address,
        @Transient val to: Address?,
        @Transient val amount: BigInteger?,
        @Transient val gasLimit: Long?,
        @Transient val gasPrice: GasPrice?,
        @Transient val data: ByteArray?
) : LongJsonRpc(
        method = "eth_estimateGas",
        params = listOf(estimateGasParams(from, to, amount, gasLimit, gasPrice, data))
) {

    companion object {
        private fun estimateGasParams(from: Address, to: Address?, amount: BigInteger?, gasLimit: Long?, gasPrice: GasPrice?, data: ByteArray?): EstimateGasParams {
            return when (gasPrice) {
                is GasPrice.Eip1559 -> {
                    EstimateGasParams.Eip1559(from, to, amount, gasLimit, gasPrice.maxFeePerGas, gasPrice.maxPriorityFeePerGas, data)
                }
                is GasPrice.Legacy -> {
                    EstimateGasParams.Legacy(from, to, amount, gasLimit, gasPrice.legacyGasPrice, data)
                }
                null -> {
                    EstimateGasParams.NoGasPrice(from, to, amount, gasLimit, data)
                }
            }
        }
    }

    private sealed class EstimateGasParams {
        data class Legacy(
                val from: Address,
                val to: Address?,
                @SerializedName("value")
                val amount: BigInteger?,
                @SerializedName("gas")
                val gasLimit: Long?,
                val gasPrice: Long?,
                val data: ByteArray?
        ) : EstimateGasParams()

        data class Eip1559(
                val from: Address,
                val to: Address?,
                @SerializedName("value")
                val amount: BigInteger?,
                @SerializedName("gas")
                val gasLimit: Long?,
                val maxFeePerGas: Long,
                val maxPriorityFeePerGas: Long,
                val data: ByteArray?
        ) : EstimateGasParams()

        data class NoGasPrice(
                val from: Address,
                val to: Address?,
                @SerializedName("value")
                val amount: BigInteger?,
                @SerializedName("gas")
                val gasLimit: Long?,
                val data: ByteArray?
        ) : EstimateGasParams()
    }
}
