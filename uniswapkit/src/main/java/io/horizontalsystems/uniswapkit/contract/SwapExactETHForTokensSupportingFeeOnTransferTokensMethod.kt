package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapExactETHForTokensSupportingFeeOnTransferTokensMethod(
        val amountOutMin: BigInteger,
        val path: List<Address>,
        val to: Address,
        val deadline: BigInteger
) : ContractMethod() {

    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(amountOutMin, path, to, deadline)

    companion object {
        const val methodSignature = "swapExactETHForTokensSupportingFeeOnTransferTokens(uint256,address[],address,uint256)"
    }

}
