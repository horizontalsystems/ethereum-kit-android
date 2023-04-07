package io.horizontalsystems.uniswapkit.v3.Quoter

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
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(tokenIn, tokenOut, fee, amountIn, sqrtPriceLimitX96)

    companion object {
        const val methodSignature = "quoteExactInputSingle(address,address,uint24,uint256,uint160)"
    }
}
