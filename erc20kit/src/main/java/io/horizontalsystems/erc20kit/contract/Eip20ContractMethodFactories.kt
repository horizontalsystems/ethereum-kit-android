package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories

object Eip20ContractMethodFactories : ContractMethodFactories() {

    init {
        registerMethodFactories(listOf(TransferMethodFactory, ApproveMethodFactory))
    }

}
