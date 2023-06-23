package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class QuoteExactOutputSingleMethod(
    val tokenIn: Address,
    val tokenOut: Address,
    val fee: BigInteger,
    val amountOut: BigInteger,
    val sqrtPriceLimitX96: BigInteger,
) : ContractMethod() {
    override val methodSignature = "quoteExactOutputSingle((address,address,uint256,uint24,uint160))"
    override fun getArguments() = listOf(tokenIn, tokenOut, amountOut, fee, sqrtPriceLimitX96)
}
