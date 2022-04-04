package io.horizontalsystems.uniswapkit

import io.horizontalsystems.erc20kit.core.getErc20EventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.uniswapkit.contract.*
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration.Token
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration.Trade
import java.math.BigInteger

class SwapTransactionDecorator(
        private val address: Address,
        private val contractMethodFactories: SwapContractMethodFactories
) : IDecorator {

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
            is SwapETHForExactTokensMethod -> {
                val amountIn: BigInteger? = null

                if (internalTransactions != null) {
                    val change = totalETHIncoming(contractMethod.to, internalTransactions)
                    transactionData.value - change
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, transactionData.value, amountIn)
                val tokenIn = Token.EvmCoin
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactETHForTokensMethod -> {
                val amountOut: BigInteger? = null

                if (eventDecorations != null) {
                    totalTokenAmount(contractMethod.to, contractMethod.path.last(), eventDecorations, false)
                }

                val trade = Trade.ExactIn(transactionData.value, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.EvmCoin
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactTokensForETHMethod -> {
                val amountOut: BigInteger? = null

                if (internalTransactions != null) {
                    totalETHIncoming(contractMethod.to, internalTransactions)
                }

                val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.EvmCoin
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactTokensForTokensMethod -> {
                val amountOut: BigInteger? = null

                if (eventDecorations != null) {
                    totalTokenAmount(contractMethod.to, contractMethod.path.last(), eventDecorations, false)
                }

                val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapTokensForExactETHMethod -> {
                val amountIn: BigInteger? = null

                if (eventDecorations != null) {
                    totalTokenAmount(contractMethod.to, contractMethod.path.first(), eventDecorations, true)
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.EvmCoin
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapTokensForExactTokensMethod -> {
                val amountIn: BigInteger? = null

                if (eventDecorations != null) {
                    totalTokenAmount(contractMethod.to, contractMethod.path.first(), eventDecorations, true)
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            else -> null
        }
    }

    private fun totalTokenAmount(userAddress: Address, tokenAddress: Address, eventDecorations: List<ContractEventDecoration>, collectIncomingAmounts: Boolean): BigInteger {
        var amountIn: BigInteger = 0.toBigInteger()
        var amountOut: BigInteger = 0.toBigInteger()

        eventDecorations.forEach { decoration ->
            if (decoration.contractAddress == tokenAddress) {
                (decoration as? TransferEventDecoration)?.let { transferEventDecoration ->
                    if (transferEventDecoration.from == userAddress) {
                        amountIn += transferEventDecoration.value
                    }
                    if (transferEventDecoration.to == userAddress) {
                        amountOut += transferEventDecoration.value
                    }
                }
            }
        }

        return if (collectIncomingAmounts) amountIn else amountOut
    }

    private fun totalETHIncoming(userAddress: Address, transactions: List<InternalTransaction>): BigInteger {
        var amountOut: BigInteger = 0.toBigInteger()

        transactions.forEach { transaction ->
            if (transaction.to == userAddress) {
                amountOut += transaction.value
            }
        }

        return amountOut
    }

}
