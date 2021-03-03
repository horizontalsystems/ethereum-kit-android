package io.horizontalsystems.uniswapkit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories

object SwapContractMethodFactories : ContractMethodFactories() {
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
        registerMethodFactories(swapContractMethodFactories)
    }
}
