package io.horizontalsystems.ethereumkit.models

sealed class GasPrice {
    class Legacy(val legacyGasPrice: Long) : GasPrice()
    class Eip1559(val maxFeePerGas: Long, val maxPriorityFeePerGas: Long) : GasPrice()

    val max: Long
        get() = when (this) {
            is Eip1559 -> maxFeePerGas
            is Legacy -> legacyGasPrice
        }

    override fun toString() = when (this) {
        is Eip1559 -> "Eip1559 [maxFeePerGas: $maxFeePerGas, maxPriorityFeePerGas: $maxPriorityFeePerGas]"
        is Legacy -> "Legacy [gasPrice: $legacyGasPrice]"
    }
}
