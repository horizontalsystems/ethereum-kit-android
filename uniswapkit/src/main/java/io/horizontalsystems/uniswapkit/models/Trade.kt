package io.horizontalsystems.uniswapkit.models

class Trade(
        val type: TradeType,
        val route: Route,
        val tokenAmountIn: TokenAmount,
        val tokenAmountOut: TokenAmount
)
