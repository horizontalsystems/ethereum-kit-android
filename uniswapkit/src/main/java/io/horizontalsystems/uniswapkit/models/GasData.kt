package io.horizontalsystems.uniswapkit.models

data class GasData(
        val gasPrice: Long,
        val swapGasLimit: Long,
        val approveGasLimit: Long? = null
)
