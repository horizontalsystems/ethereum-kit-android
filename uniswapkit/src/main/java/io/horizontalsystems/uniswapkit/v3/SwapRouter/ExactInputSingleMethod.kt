package io.horizontalsystems.uniswapkit.v3.SwapRouter

import io.horizontalsystems.ethereumkit.contracts.ContractMethod

class ExactInputSingleMethod(
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf<Any>()

    companion object {
        const val methodSignature = "exactInputSingle"
    }
}
