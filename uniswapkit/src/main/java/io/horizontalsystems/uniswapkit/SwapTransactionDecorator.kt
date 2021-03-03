package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.core.TransactionDecoration
import io.horizontalsystems.ethereumkit.core.TransactionDecoration.Swap
import io.horizontalsystems.ethereumkit.core.TransactionDecoration.Swap.Trade
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.uniswapkit.contract.*

class SwapTransactionDecorator(
        private val contractMethodFactories: SwapContractMethodFactories
) : IDecorator {

    override fun decorate(transactionData: TransactionData): TransactionDecoration? =
            when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
                is SwapETHForExactTokensMethod -> {
                    val trade = Trade.ExactOut(contractMethod.amountOut, transactionData.value)
                    val tokenIn = Swap.Token.EvmCoin
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactETHForTokensMethod -> {
                    val trade = Trade.ExactIn(transactionData.value, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.EvmCoin
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactETHForTokensSupportingFeeOnTransferTokensMethod -> {
                    val trade = Trade.ExactIn(transactionData.value, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.EvmCoin
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForETHMethod -> {
                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.EvmCoin
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForETHSupportingFeeOnTransferTokensMethod -> {
                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.EvmCoin
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForTokensMethod -> {
                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForTokensSupportingFeeOnTransferTokensMethod -> {
                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapTokensForExactETHMethod -> {
                    val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.EvmCoin
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapTokensForExactTokensMethod -> {
                    val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax)
                    val tokenIn = Swap.Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Swap.Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                else -> null
            }
}
