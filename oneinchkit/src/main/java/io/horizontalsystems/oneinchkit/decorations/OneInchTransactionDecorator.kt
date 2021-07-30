package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.erc20kit.core.getErc20Event
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.oneinchkit.contracts.OneInchContractMethodFactories
import io.horizontalsystems.oneinchkit.contracts.SwapMethod
import io.horizontalsystems.oneinchkit.contracts.UnoswapMethod
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration.Token
import java.math.BigInteger

class OneInchTransactionDecorator(
        private val address: Address,
        private val contractMethodFactories: OneInchContractMethodFactories
) : IDecorator {

    private val evmTokenAddresses = mutableListOf("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE", "0x0000000000000000000000000000000000000000")

    override fun decorate(logs: List<TransactionLog>): List<ContractEventDecoration> {
        return listOf()
    }

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): ContractMethodDecoration? {
        val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input) ?: return null

        if (fullTransaction != null && fullTransaction.transaction.from != address) {
            // We only parse transactions created by the user (owner of this wallet).
            // If a swap was initiated by someone else and "recipient" is set to user's it should be shown as just an incoming transaction
            return null
        }

        return when (contractMethod) {
            is UnoswapMethod -> {
                var toToken: Token? = null
                var toAmount: BigInteger? = null
                fullTransaction?.let {
                    totalETHIncoming(address, fullTransaction.internalTransactions)?.let { amount ->
                        toAmount = amount
                        toToken = Token.EvmCoin
                    }
                }

                val logs = fullTransaction?.receiptWithLogs?.logs
                if (toToken == null && logs != null) {
                    val incomingEip20Log = logs.firstOrNull { log ->
                        (log.getErc20Event() as? TransferEventDecoration)?.to == address
                    }

                    (incomingEip20Log?.getErc20Event() as? TransferEventDecoration)?.let { transferEventDecoration ->
                        totalTokenIncoming(address, transferEventDecoration.contractAddress, logs)?.let { amount ->
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

                if (fullTransaction != null && toToken == Token.EvmCoin) {
                    totalETHIncoming(swapDescription.dstReceiver, fullTransaction.internalTransactions)?.let { amount ->
                        toAmount = amount
                    }
                }

                if (toAmount == null) {
                    fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                        totalTokenIncoming(swapDescription.dstReceiver, swapDescription.dstToken, logs)?.let { amount ->
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
            else -> null
        }

    }

    private fun totalTokenIncoming(userAddress: Address, tokenAddress: Address, logs: List<TransactionLog>): BigInteger? {
        var amountOut = BigInteger.ZERO

        for (log in logs) {
            if (log.address == tokenAddress) {
                (log.getErc20Event() as? TransferEventDecoration)?.let { transferEventDecoration ->
                    if (transferEventDecoration.to == userAddress) {
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
