package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.uniswapkit.RouteError

class Route(val pairs: List<Pair>,
            tokenIn: Token,
            tokenOut: Token) {

    val path: List<Token>
    val midPrice: Price

    init {
        check(pairs.isNotEmpty()) {
            throw RouteError.EmptyPairs()
        }

        val path = mutableListOf(tokenIn)
        var currentTokenIn = tokenIn

        pairs.forEachIndexed { index, pair ->
            check(pair.involves(currentTokenIn)) {
                throw RouteError.InvalidPair(index)
            }

            val currentTokenOut = pair.other(currentTokenIn)
            path.add(currentTokenOut)
            currentTokenIn = currentTokenOut

            if (index == pairs.size - 1) {
                check(currentTokenOut == tokenOut) {
                    throw RouteError.InvalidPair(index)
                }
            }
        }

        this.path = path
        val prices = mutableListOf<Price>()
        pairs.forEachIndexed { index, pair ->
            val price = if (path[index] == pair.token0)
                Price(pair.reserve0, pair.reserve1) else
                Price(pair.reserve1, pair.reserve0)
            prices.add(price)
        }
        this.midPrice = prices.reduce { acc, price -> acc * price }
    }

}
