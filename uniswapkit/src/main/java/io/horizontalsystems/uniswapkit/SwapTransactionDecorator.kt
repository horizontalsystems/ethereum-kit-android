package io.horizontalsystems.uniswapkit

import io.horizontalsystems.erc20kit.core.getErc20Event
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

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): ContractMethodDecoration? {

        if (fullTransaction != null && fullTransaction.transaction.from != address) {
            // We only parse transactions created by the user (owner of this wallet).
            // If a swap was initiated by someone else and "recipient" is set to user's it should be shown as just an incoming transaction
            return null
        }

        return when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
            is SwapETHForExactTokensMethod -> {
                val amountIn: BigInteger? = fullTransaction?.let { fullTx ->
                    val change = totalETHIncoming(contractMethod.to, fullTransaction.internalTransactions)
                    fullTx.transaction.value - change
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, transactionData.value, amountIn)
                val tokenIn = Token.EvmCoin
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactETHForTokensMethod -> {
                val amountOut: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                    totalTokenAmount(contractMethod.to, contractMethod.path.last(), logs, false)
                }

                val trade = Trade.ExactIn(transactionData.value, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.EvmCoin
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactTokensForETHMethod -> {
                val amountOut: BigInteger? = fullTransaction?.internalTransactions?.let { internalTransactions ->
                    totalETHIncoming(contractMethod.to, internalTransactions)
                }

                val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.EvmCoin
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapExactTokensForTokensMethod -> {
                val amountOut: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                    totalTokenAmount(contractMethod.to, contractMethod.path.last(), logs, false)
                }

                val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapTokensForExactETHMethod -> {
                val amountIn: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                    totalTokenAmount(contractMethod.to, contractMethod.path.first(), logs, true)
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.EvmCoin
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            is SwapTokensForExactTokensMethod -> {
                val amountIn: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                    totalTokenAmount(contractMethod.to, contractMethod.path.first(), logs, true)
                }

                val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                SwapMethodDecoration(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
            }
            else -> null
        }
    }

    override fun decorate(logs: List<TransactionLog>): List<ContractEventDecoration> {
        return emptyList()
    }

    private fun totalTokenAmount(userAddress: Address, tokenAddress: Address, logs: List<TransactionLog>, collectIncomingAmounts: Boolean): BigInteger {
        var amountIn: BigInteger = 0.toBigInteger()
        var amountOut: BigInteger = 0.toBigInteger()

        logs.forEach { log ->
            if (log.address == tokenAddress) {
                (log.getErc20Event() as? TransferEventDecoration)?.let { transferEventDecoration ->
                    if (transferEventDecoration.from == userAddress) {
                        amountIn += transferEventDecoration.value
                        log.relevant = true
                    }
                    if (transferEventDecoration.to == userAddress) {
                        amountOut += transferEventDecoration.value
                        log.relevant = true
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
