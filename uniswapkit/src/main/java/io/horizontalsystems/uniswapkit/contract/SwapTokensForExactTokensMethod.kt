package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapTokensForExactTokensMethod(
        val amountOut: BigInteger,
        val amountInMax: BigInteger,
        val path: List<Address>,
        val to: Address,
        val deadline: BigInteger
) : ContractMethod() {

    override val methodSignature = "swapTokensForExactTokens(uint256,uint256,address[],address,uint256)"
    override fun getArguments() = listOf(amountOut, amountInMax, path, to, deadline)

}
