package io.horizontalsystems.uniswapkit.models

data class TradeOptions(
        var allowedSlippage: Double = 0.5,
        var ttl: Long = 20 * 60,
        var recipient: ByteArray? = null,
        var feeOnTransfer: Boolean = false
)
