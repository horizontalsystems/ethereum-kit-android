package io.horizontalsystems.uniswapkit.v3.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.uniswapkit.v3.router.*

object UniswapV3ContractMethodFactories : ContractMethodFactories() {
    init {
        val swapContractMethodFactories = listOf(
            ExactInputMethod.Factory(),
            ExactOutputMethod.Factory(),
            ExactInputSingleMethod.Factory(),
            ExactOutputSingleMethod.Factory(),
            UnwrapWETH9Method.Factory(),
            MulticallMethod.Factory(UniswapV3ContractMethodFactories),
        )
        registerMethodFactories(swapContractMethodFactories)
    }
}
