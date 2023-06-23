package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class QuoteExactInputSingleMethod(
    val tokenIn: Address,
    val tokenOut: Address,
    val fee: BigInteger,
    val amountIn: BigInteger,
    val sqrtPriceLimitX96: BigInteger,
) : ContractMethod() {
    override val methodSignature = "quoteExactInputSingle((address,address,uint256,uint24,uint160))"
    override fun getArguments() = listOf(tokenIn, tokenOut, amountIn, fee, sqrtPriceLimitX96)
}
