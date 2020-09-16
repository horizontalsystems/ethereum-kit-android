package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapETHForExactTokensMethod(
        val amountOut: BigInteger,
        val path: List<Address>,
        val to: Address,
        val deadline: BigInteger
) : ContractMethod() {

    override val methodSignature = "swapETHForExactTokens(uint256,address[],address,uint256)"
    override fun getArguments() = listOf(amountOut, path, to, deadline)

}
