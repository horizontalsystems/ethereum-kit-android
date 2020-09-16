package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapExactTokensForTokensSupportingFeeOnTransferTokensMethod(
        val amountIn: BigInteger,
        val amountOutMin: BigInteger,
        val path: List<Address>,
        val to: Address,
        val deadline: BigInteger
) : ContractMethod() {

    override val methodSignature = "swapExactTokensForTokensSupportingFeeOnTransferTokens(uint256,uint256,address[],address,uint256)"
    override fun getArguments() = listOf(amountIn, amountOutMin, path, to, deadline)

}
