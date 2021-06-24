package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.Eip20ContractMethodFactories
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.decorations.ApproveEventDecoration
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog


class Eip20TransactionDecorator(
        private val userAddress: Address,
        private val contractMethodFactories: Eip20ContractMethodFactories
) : IDecorator {

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): ContractMethodDecoration? =
            when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
                is TransferMethod -> TransferMethodDecoration(contractMethod.to, contractMethod.value)
                is ApproveMethod -> ApproveMethodDecoration(contractMethod.spender, contractMethod.value)
                else -> null
            }

    override fun decorate(logs: List<TransactionLog>): List<ContractEventDecoration> {
        return logs.mapNotNull { log ->

            val event = log.getErc20Event() ?: return@mapNotNull null

            when (event) {
                is TransferEventDecoration -> {
                    if (event.from == userAddress || event.to == userAddress) {
                        return@mapNotNull event
                    }
                }
                is ApproveEventDecoration -> {
                    if (event.owner == userAddress || event.spender == userAddress) {
                        return@mapNotNull event
                    }
                }
            }

            return@mapNotNull null
        }
    }
}
