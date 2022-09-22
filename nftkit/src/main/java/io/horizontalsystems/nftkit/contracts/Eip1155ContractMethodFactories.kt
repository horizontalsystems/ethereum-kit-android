package io.horizontalsystems.nftkit.contracts

import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories

object Eip1155ContractMethodFactories : ContractMethodFactories() {
    init {
        registerMethodFactories(listOf(Eip1155SafeTransferFromMethodFactory()))
    }
}