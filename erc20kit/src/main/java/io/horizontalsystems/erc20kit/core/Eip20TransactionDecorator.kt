package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.Eip20ContractMethodFactories
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.events.ApproveEventDecoration
import io.horizontalsystems.erc20kit.events.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.models.TransactionLog


class Eip20TransactionDecorator(
        private val userAddress: Address,
        private val tokenAddress: Address,
        private val contractMethodFactories: Eip20ContractMethodFactories
) : IDecorator {

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): TransactionDecoration? =
            when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
                is TransferMethod -> TransactionDecoration.Eip20Transfer(contractMethod.to, contractMethod.value)
                is ApproveMethod -> TransactionDecoration.Eip20Approve(contractMethod.spender, contractMethod.value)
                else -> null
            }

    override fun decorate(logs: List<TransactionLog>): List<EventDecoration> {
        return logs.mapNotNull { log ->

            val event = if (log.address == tokenAddress) {
                log.getErc20Event()
            } else {
                return@mapNotNull null
            }

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
