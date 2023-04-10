package io.horizontalsystems.uniswapkit.v3.SwapRouter

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ExactOutputSingleMethod(
    val tokenIn: Address,
    val tokenOut: Address,
    val fee: BigInteger,
    val recipient: Address,
    val deadline: BigInteger,
    val amountOut: BigInteger,
    val amountInMaximum: BigInteger,
    val sqrtPriceLimitX96: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(
        tokenIn,
        tokenOut,
        fee,
        recipient,
        deadline,
        amountOut,
        amountInMaximum,
        sqrtPriceLimitX96
    )

    companion object {
        const val methodSignature =
            "exactOutputSingle((address,address,uint24,address,uint256,uint256,uint256,uint160))"
    }
}
