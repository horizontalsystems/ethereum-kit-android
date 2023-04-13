package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger

data class BestTradeExactOut(val swapPath: SwapPath, val amountIn: BigInteger)
