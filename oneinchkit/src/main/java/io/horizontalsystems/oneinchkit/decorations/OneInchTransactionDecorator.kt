package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.erc20kit.core.getErc20EventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.oneinchkit.contracts.OneInchContractMethodFactories
import io.horizontalsystems.oneinchkit.contracts.OneInchV4Method
import io.horizontalsystems.oneinchkit.contracts.SwapMethod
import io.horizontalsystems.oneinchkit.contracts.UnoswapMethod
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration.Token
import java.math.BigInteger

class OneInchTransactionDecorator(
        private val address: Address,
        private val contractMethodFactories: OneInchContractMethodFactories
) : IDecorator {

    private val evmTokenAddresses = mutableListOf("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE", "0x0000000000000000000000000000000000000000")

    override fun decorate(transactionData: TransactionData): ContractMethodDecoration? =
        getMethodDecoration(transactionData)

    override fun decorate(fullTransaction: FullTransaction, fullRpcTransaction: FullRpcTransaction) {
        decorateMain(fullTransaction, fullRpcTransaction.rpcReceipt.logs.mapNotNull { it.getErc20EventDecoration() })
    }

    override fun decorateTransactions(fullTransactions: Map<String, FullTransaction>) {
        for (fullTransaction in fullTransactions.values) {
            decorateMain(fullTransaction, fullTransaction.eventDecorations)
        }
    }

    private fun decorateMain(fullTransaction: FullTransaction, eventDecorations: List<ContractEventDecoration>) {
        if (fullTransaction.transaction.from != address) {
            // We only parse transactions created by the user (owner of this wallet).
            // If a swap was initiated by someone else and "recipient" is set to user's it should be shown as just an incoming transaction
            return
        }

        val transactionData = fullTransaction.transactionData ?: return
        val decoration = getMethodDecoration(transactionData, fullTransaction.internalTransactions, eventDecorations) ?: return

        fullTransaction.mainDecoration = decoration
    }

    private fun getMethodDecoration(transactionData: TransactionData, internalTransactions: List<InternalTransaction>? = null, eventDecorations: List<ContractEventDecoration>? = null): ContractMethodDecoration? {
        return when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
            is UnoswapMethod -> {
                var toToken: Token? = null
                var toAmount: BigInteger? = null

                if (internalTransactions != null) {
                    totalETHIncoming(address, internalTransactions)?.let { amount ->
                        toAmount = amount
                        toToken = Token.EvmCoin
                    }
                }

                if (toToken == null && eventDecorations != null) {
                    val incomingEip20EventDecoration = eventDecorations.firstOrNull { eventDecoration ->
                        (eventDecoration as? TransferEventDecoration)?.to == address
                    }

                    incomingEip20EventDecoration?.let { transferEventDecoration ->
                        totalTokenIncoming(address, transferEventDecoration.contractAddress, eventDecorations)?.let { amount ->
                            toAmount = amount
                            toToken = Token.Eip20(transferEventDecoration.contractAddress)
                        }
                    }
                }

                OneInchUnoswapMethodDecoration(
                        fromToken = addressToToken(contractMethod.srcToken),
                        toToken = toToken,
                        fromAmount = contractMethod.amount,
                        toAmountMin = contractMethod.minReturn,
                        toAmount = toAmount,
                        params = contractMethod.params
                )
            }
            is SwapMethod -> {
                var toAmount: BigInteger? = null
                val swapDescription = contractMethod.swapDescription
                val toToken = addressToToken(swapDescription.dstToken)

                if (internalTransactions != null && toToken == Token.EvmCoin) {
                    totalETHIncoming(swapDescription.dstReceiver, internalTransactions)?.let { amount ->
                        toAmount = amount
                    }
                }

                if (toAmount == null) {
                    eventDecorations?.let { decorations ->
                        totalTokenIncoming(swapDescription.dstReceiver, swapDescription.dstToken, decorations)?.let { amount ->
                            toAmount = amount
                        }
                    }
                }
                OneInchSwapMethodDecoration(
                        fromToken = addressToToken(swapDescription.srcToken),
                        toToken = toToken,
                        fromAmount = swapDescription.amount,
                        toAmountMin = swapDescription.minReturnAmount,
                        toAmount = toAmount,
                        flags = swapDescription.flags,
                        permit = swapDescription.permit,
                        data = contractMethod.data,
                        recipient = swapDescription.dstReceiver
                )
            }
            is OneInchV4Method -> {
                OneInchV4MethodDecoration()
            }
            else -> null
        }

    }

    private fun totalTokenIncoming(userAddress: Address, tokenAddress: Address, eventDecorations: List<ContractEventDecoration>): BigInteger? {
        var amountOut = BigInteger.ZERO

        for (decoration in eventDecorations) {
            if (decoration.contractAddress == tokenAddress) {
                (decoration as? TransferEventDecoration)?.let { transferEventDecoration ->
                    if (transferEventDecoration.to == userAddress && transferEventDecoration.value > BigInteger.ZERO) {
                        amountOut += transferEventDecoration.value
                    }
                }
            }
        }

        return if (amountOut > BigInteger.ZERO) amountOut else null
    }

    private fun totalETHIncoming(userAddress: Address, transactions: List<InternalTransaction>): BigInteger? {
        var amountOut = BigInteger.ZERO

        for (transaction in transactions) {
            if (transaction.to == userAddress) {
                amountOut += transaction.value
            }
        }

        return if (amountOut > BigInteger.ZERO) amountOut else null
    }

    private fun addressToToken(address: Address) = if (evmTokenAddresses.contains(address.eip55))
        Token.EvmCoin
    else
        Token.Eip20(address)

}
