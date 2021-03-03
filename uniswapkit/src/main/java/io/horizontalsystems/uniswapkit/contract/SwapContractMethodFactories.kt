package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.spv.core.toInt

object SwapContractMethodFactories {
    private val methodFactories = mutableMapOf<Int, ContractMethodFactory>()

    init {
        val swapContractMethodFactories = listOf(
                SwapETHForExactTokensMethodFactory,
                SwapExactETHForTokensMethodFactory,
                SwapExactETHForTokensSupportingFeeOnTransferTokensMethodFactory,
                SwapExactTokensForETHMethodFactory,
                SwapExactTokensForETHSupportingFeeOnTransferTokensMethodFactory,
                SwapExactTokensForTokensMethodFactory,
                SwapExactTokensForTokensSupportingFeeOnTransferTokensMethodFactory,
                SwapTokensForExactETHMethodFactory,
                SwapTokensForExactTokensMethodFactory
        )
        swapContractMethodFactories.forEach { factory ->
            methodFactories[factory.methodId.toInt()] = factory
        }
    }

    fun createMethodFromInput(input: ByteArray): ContractMethod? {
        val methodId = input.copyOfRange(0, 4)
        val methodFactory = methodFactories[methodId.toInt()]

        return methodFactory?.createMethod(input.copyOfRange(4, input.size))
    }

}
