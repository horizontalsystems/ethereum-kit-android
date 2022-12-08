package io.horizontalsystems.oneinchkit.contracts

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.oneinchkit.contracts.v4.UnparsedSwapMethodsFactoryV4
import io.horizontalsystems.oneinchkit.contracts.v4.SwapMethodFactoryV4
import io.horizontalsystems.oneinchkit.contracts.v4.UnoswapMethodFactoryV4

object OneInchContractMethodFactories : ContractMethodFactories() {

    init {
        val swapContractMethodFactories = listOf(UnoswapMethodFactoryV4(), SwapMethodFactoryV4(), UnparsedSwapMethodsFactoryV4())
        registerMethodFactories(swapContractMethodFactories)
    }

}
