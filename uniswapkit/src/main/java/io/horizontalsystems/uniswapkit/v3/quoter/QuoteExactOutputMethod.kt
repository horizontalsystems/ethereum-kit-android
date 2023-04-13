package io.horizontalsystems.uniswapkit.v3.quoter

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger

class QuoteExactOutputMethod(
    val path: SwapPath,
    val amountOut: BigInteger,
) : ContractMethod() {

    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(path.abiEncodePacked(), amountOut)

    companion object {
        const val methodSignature = "quoteExactOutput(bytes,uint256)"
    }
}
