package io.horizontalsystems.uniswapkit.models

data class SwapData(
        val pairs: List<Pair>,
        val tokenIn: Token,
        val tokenOut: Token
) {
    override fun toString(): String {
        val pairsInfo = pairs.joinToString("\n")
        return "SwapData {tokenIn: $tokenIn; tokenOut: $tokenOut}\n$pairsInfo"
    }
}
