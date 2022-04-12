package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import java.math.BigInteger


class Eip20TransactionDecorator(
    private val userAddress: Address
) : ITransactionDecorator {

    override fun decoration(from: Address?, to: Address?, value: BigInteger?, contractMethod: ContractMethod?, internalTransactions: List<InternalTransaction>, eventInstances: List<ContractEventInstance>): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        if (contractMethod is TransferMethod && from == userAddress) {
            return OutgoingEip20Decoration(
                to,
                contractMethod.to,
                contractMethod.value,
                contractMethod.to == userAddress
            )
        }

        if (contractMethod is ApproveMethod) {
            return ApproveEip20Decoration(
                to,
                contractMethod.spender,
                contractMethod.value
            )
        }

        return null
    }
//
//    override fun decorate(transactionData: TransactionData): ContractMethodDecoration? =
//        when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
//            is TransferMethod -> TransferMethodDecoration(contractMethod.to, contractMethod.value)
//            is ApproveMethod -> ApproveMethodDecoration(contractMethod.spender, contractMethod.value)
//            else -> null
//        }
//
//    override fun decorate(fullTransaction: FullTransaction, fullRpcTransaction: FullRpcTransaction) {
//        decorateMain(fullTransaction)
//        decorateLogs(fullTransaction, fullRpcTransaction.rpcReceipt.logs)
//    }
//
//    override fun decorateTransactions(fullTransactions: Map<String, FullTransaction>) {
//        for (fullTransaction in fullTransactions.values) {
//            decorateMain(fullTransaction)
//        }
//
//        decorateEvents(fullTransactions)
//    }
//
//    private fun decorateMain(fullTransaction: FullTransaction) {
//        val transactionData = fullTransaction.transactionData ?: return
//        val decoration = decorate(transactionData) ?: return
//
//        fullTransaction.mainDecoration = decoration
//    }
//
//    private fun decorateLogs(fullTransaction: FullTransaction, logs: List<TransactionLog>) {
//        val eventDecorations = logs.mapNotNull { log ->
//            val event = log.getErc20EventInstance() ?: return@mapNotNull null
//
//            when (event) {
//                is TransferEventInstance -> {
//                    if (event.from == userAddress || event.to == userAddress) {
//                        return@mapNotNull event
//                    }
//                }
//                is ApproveEventInstance -> {
//                    if (event.owner == userAddress || event.spender == userAddress) {
//                        return@mapNotNull event
//                    }
//                }
//            }
//
//            return@mapNotNull null
//        }
//
//        fullTransaction.eventDecorations.addAll(eventDecorations)
//    }
//
//    private fun decorateEvents(fullTransactions: Map<String, FullTransaction>) {
//        val erc20Events = if (fullTransactions.size > 100) {
//            storage.getEvents()
//        } else {
//            storage.getEventsByHashes(fullTransactions.values.map { it.transaction.hash })
//        }
//
//        for (event in erc20Events) {
//            val decoration = TransferEventInstance(event.contractAddress, event.from, event.to, event.value, event.tokenName, event.tokenSymbol, event.tokenDecimal)
//            fullTransactions[event.hashString]?.eventDecorations?.add(decoration)
//        }
//    }

}
