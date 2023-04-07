package io.horizontalsystems.uniswapkit.v3.Quoter

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
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(tokenIn, tokenOut, fee, amountOut, sqrtPriceLimitX96)

    companion object {
        const val methodSignature = "quoteExactOutputSingle(address,address,uint24,uint256,uint160)"
    }
}
