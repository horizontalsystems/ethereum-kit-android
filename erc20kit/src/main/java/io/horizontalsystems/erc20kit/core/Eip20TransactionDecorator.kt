package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.Eip20ContractMethodFactories
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.TransactionData


class Eip20TransactionDecorator(
        private val contractMethodFactories: Eip20ContractMethodFactories
) : IDecorator {

    override fun decorate(transactionData: TransactionData): TransactionDecoration? =
            when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
                is TransferMethod -> {
                    TransactionDecoration.Eip20Transfer(contractMethod.to, contractMethod.value, transactionData.to)
                }
                is ApproveMethod -> {
                    TransactionDecoration.Eip20Approve(contractMethod.spender, contractMethod.value, transactionData.to)
                }
                else -> null
            }
}
