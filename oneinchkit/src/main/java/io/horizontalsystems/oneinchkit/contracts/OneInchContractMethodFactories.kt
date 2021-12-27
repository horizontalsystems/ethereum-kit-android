package io.horizontalsystems.oneinchkit.contracts

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories

object OneInchContractMethodFactories : ContractMethodFactories() {

    init {
        val swapContractMethodFactories = listOf(UnoswapMethodFactory(), SwapMethodFactory(), OneInchV4MethodsFactory())
        registerMethodFactories(swapContractMethodFactories)
    }

}
