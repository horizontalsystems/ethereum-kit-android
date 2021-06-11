package io.horizontalsystems.uniswapkit

import io.horizontalsystems.erc20kit.core.getErc20Event
import io.horizontalsystems.erc20kit.events.TransferEventDecoration
import io.horizontalsystems.ethereumkit.core.IDecorator
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration.*
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration.Swap.*
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.uniswapkit.contract.*
import io.horizontalsystems.uniswapkit.decorations.SwapEventDecoration
import java.math.BigInteger

class SwapTransactionDecorator(
        private val userAddress: Address,
        private val contractMethodFactories: SwapContractMethodFactories
) : IDecorator {

    override fun decorate(transactionData: TransactionData, fullTransaction: FullTransaction?): TransactionDecoration? =
            when (val contractMethod = contractMethodFactories.createMethodFromInput(transactionData.input)) {
                is SwapETHForExactTokensMethod -> {
                    val amountIn: BigInteger? = fullTransaction?.let { fullTx ->
                        val change = totalETHIncoming(contractMethod.to, fullTransaction.internalTransactions)
                        fullTx.transaction.value - change
                    }

                    val trade = Trade.ExactOut(contractMethod.amountOut, transactionData.value, amountIn)
                    val tokenIn = Token.EvmCoin
                    val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactETHForTokensMethod -> {
                    val amountOut: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                        totalTokenAmount(contractMethod.to, contractMethod.path.last(), logs, false)
                    }

                    val trade = Trade.ExactIn(transactionData.value, contractMethod.amountOutMin, amountOut)
                    val tokenIn = Token.EvmCoin
                    val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForETHMethod -> {
                    val amountOut: BigInteger? = fullTransaction?.internalTransactions?.let { internalTransactions ->
                        totalETHIncoming(contractMethod.to, internalTransactions)
                    }

                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                    val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Token.EvmCoin
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapExactTokensForTokensMethod -> {
                    val amountOut: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                        totalTokenAmount(contractMethod.to, contractMethod.path.last(), logs, false)
                    }

                    val trade = Trade.ExactIn(contractMethod.amountIn, contractMethod.amountOutMin, amountOut)
                    val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapTokensForExactETHMethod -> {
                    val amountIn: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                        totalTokenAmount(contractMethod.to, contractMethod.path.first(), logs, true)
                    }

                    val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                    val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Token.EvmCoin
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                is SwapTokensForExactTokensMethod -> {
                    val amountIn: BigInteger? = fullTransaction?.receiptWithLogs?.logs?.let { logs ->
                        totalTokenAmount(contractMethod.to, contractMethod.path.first(), logs, true)
                    }

                    val trade = Trade.ExactOut(contractMethod.amountOut, contractMethod.amountInMax, amountIn)
                    val tokenIn = Token.Eip20Coin(contractMethod.path.first())
                    val tokenOut = Token.Eip20Coin(contractMethod.path.last())
                    Swap(trade, tokenIn, tokenOut, contractMethod.to, contractMethod.deadline)
                }
                else -> null
            }

    override fun decorate(logs: List<TransactionLog>): List<EventDecoration> {
        return logs.mapNotNull { log ->
            val signature = log.topics[0].hexStringToByteArrayOrNull()

            if (signature.contentEquals(SwapEventDecoration.signature) && log.topics.size == 3 && log.data.size == 128) {
                val firstParam = Address(log.topics[1])
                val secondParam = Address(log.topics[2])

                if (firstParam == userAddress || secondParam == userAddress) {
                    val amount0In = BigInteger(log.data.copyOfRange(0, 32))
                    val amount1In = BigInteger(log.data.copyOfRange(32, 64))
                    val amount0Out = BigInteger(log.data.copyOfRange(64, 96))
                    val amount1Out = BigInteger(log.data.copyOfRange(96, 128))

                    return@mapNotNull SwapEventDecoration(
                            contractAddress = log.address,
                            sender = firstParam,
                            amount0In = amount0In,
                            amount1In = amount1In,
                            amount0Out = amount0Out,
                            amount1Out = amount1Out,
                            to = secondParam
                    )
                }
            }

            return@mapNotNull null
        }
    }

    private fun totalTokenAmount(userAddress: Address, tokenAddress: Address, logs: List<TransactionLog>, collectIncomingAmounts: Boolean): BigInteger {
        var amountIn: BigInteger = 0.toBigInteger()
        var amountOut: BigInteger = 0.toBigInteger()

        logs.forEach { log ->
            if (log.address == tokenAddress) {
                (log.getErc20Event() as? TransferEventDecoration)?.let { transferEventDecoration ->
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
