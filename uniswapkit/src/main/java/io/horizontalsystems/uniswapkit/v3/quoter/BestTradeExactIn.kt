package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger

data class BestTradeExactIn(val swapPath: SwapPath, val amountOut: BigInteger)
