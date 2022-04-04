package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.Eip20ContractMethodFactories
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.decorations.ApproveEventDecoration
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.core.IEip20Storage
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.*


class Eip20TransactionDecorator(
        private val userAddress: Address,
        private val contractMethodFactories: Eip20ContractMethodFactories,
        private val storage: IEip20Storage
) : IDecorator {

    override fun decorate(transactionData: TransactionData): ContractMethodDecoration? =
        when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
            is TransferMethod -> TransferMethodDecoration(contractMethod.to, contractMethod.value)
            is ApproveMethod -> ApproveMethodDecoration(contractMethod.spender, contractMethod.value)
            else -> null
        }

    override fun decorate(fullTransaction: FullTransaction, fullRpcTransaction: FullRpcTransaction) {
        decorateMain(fullTransaction)
        decorateLogs(fullTransaction, fullRpcTransaction.rpcReceipt.logs)
    }

    override fun decorateTransactions(fullTransactions: Map<String, FullTransaction>) {
        for (fullTransaction in fullTransactions.values) {
            decorateMain(fullTransaction)
        }

        decorateEvents(fullTransactions)
    }

    private fun decorateMain(fullTransaction: FullTransaction) {
        val transactionData = fullTransaction.transactionData ?: return
        val decoration = decorate(transactionData) ?: return

        fullTransaction.mainDecoration = decoration
    }

    private fun decorateLogs(fullTransaction: FullTransaction, logs: List<TransactionLog>) {
        val eventDecorations = logs.mapNotNull { log ->
            val event = log.getErc20EventDecoration() ?: return@mapNotNull null

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

        fullTransaction.eventDecorations.addAll(eventDecorations)
    }

    private fun decorateEvents(fullTransactions: Map<String, FullTransaction>) {
        val erc20Events = if (fullTransactions.size > 100) {
            storage.getEvents()
        } else {
            storage.getEventsByHashes(fullTransactions.values.map { it.transaction.hash })
        }

        for (event in erc20Events) {
            val decoration = TransferEventDecoration(event.contractAddress, event.from, event.to, event.value, event.tokenName, event.tokenSymbol, event.tokenDecimal)
            fullTransactions[event.hashString]?.eventDecorations?.add(decoration)
        }
    }

}
