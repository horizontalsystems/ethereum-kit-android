package io.horizontalsystems.uniswapkit.models

class Route(val pairs: List<Pair>,
            val tokenIn: Token,
            val tokenOut: Token) {

    val path: List<Token>

    init {
        check(pairs.isNotEmpty()) {
            throw EmptyPairs()
        }

        val path = mutableListOf(tokenIn)
        var currentTokenIn = tokenIn

        pairs.forEachIndexed { index, pair ->
            check(pair.involves(currentTokenIn)) {
                throw InvalidPair(index)
            }

            val currentTokenOut = pair.other(currentTokenIn)
            path.add(currentTokenOut)
            currentTokenIn = currentTokenOut

            if (index == pairs.size - 1) {
                check(currentTokenOut == tokenOut) {
                    throw InvalidPair(index)
                }
            }
        }

        this.path = path
    }

    open class RouteError : Exception()
    class EmptyPairs : RouteError()
    class InvalidPair(val index: Int) : RouteError()

}
