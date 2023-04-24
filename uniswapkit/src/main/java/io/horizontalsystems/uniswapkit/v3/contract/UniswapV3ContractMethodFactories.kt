package io.horizontalsystems.uniswapkit.v3.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.uniswapkit.contract.*
import io.horizontalsystems.uniswapkit.v3.router.ExactInputMethod
import io.horizontalsystems.uniswapkit.v3.router.ExactInputSingleMethod
import io.horizontalsystems.uniswapkit.v3.router.ExactOutputMethod
import io.horizontalsystems.uniswapkit.v3.router.ExactOutputSingleMethod

object UniswapV3ContractMethodFactories : ContractMethodFactories() {
    init {
        val swapContractMethodFactories = listOf(
            ExactInputMethod.Factory(),
            ExactOutputMethod.Factory(),
            ExactInputSingleMethod.Factory(),
            ExactOutputSingleMethod.Factory(),
        )
        registerMethodFactories(swapContractMethodFactories)
    }
}
