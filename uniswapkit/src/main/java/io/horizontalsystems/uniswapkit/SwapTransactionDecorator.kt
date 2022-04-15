package io.horizontalsystems.uniswapkit

import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.uniswapkit.contract.*
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import java.math.BigInteger

class SwapTransactionDecorator : ITransactionDecorator {

    override fun decoration(from: Address?, to: Address?, value: BigInteger?, contractMethod: ContractMethod?, internalTransactions: List<InternalTransaction>, eventInstances: List<ContractEventInstance>): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        when (contractMethod) {
            is SwapETHForExactTokensMethod -> {
                val lastCoinInPath = contractMethod.path.lastOrNull() ?: return null

                val amountIn = if (internalTransactions.isEmpty()) {
                    SwapDecoration.Amount.Extremum(value)
                } else {
                    val change = totalETHIncoming(contractMethod.to, internalTransactions)
                    SwapDecoration.Amount.Exact(value - change)
                }

                return SwapDecoration(
                    to,
                    amountIn,
                    SwapDecoration.Amount.Exact(contractMethod.amountOut),
                    SwapDecoration.Token.EvmCoin,
                    findEip20Token(eventInstances, lastCoinInPath),
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline
                )
            }

            is SwapExactETHForTokensMethod -> {
                val lastCoinInPath = contractMethod.path.lastOrNull() ?: return null

                val amountOut = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(contractMethod.amountOutMin)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(contractMethod.to, lastCoinInPath, eventInstances, false))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(value),
                    amountOut,
                    SwapDecoration.Token.EvmCoin,
                    findEip20Token(eventInstances, lastCoinInPath),
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline
                )
            }

            is SwapExactTokensForETHMethod -> {
                val firstCoinInPath = contractMethod.path.firstOrNull() ?: return null

                val amountOut = if (internalTransactions.isEmpty()) {
                    SwapDecoration.Amount.Extremum(contractMethod.amountOutMin)
                } else {
                    SwapDecoration.Amount.Exact(totalETHIncoming(contractMethod.to, internalTransactions))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(contractMethod.amountIn),
                    amountOut,
                    findEip20Token(eventInstances, firstCoinInPath),
                    SwapDecoration.Token.EvmCoin,
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline
                )
            }

            is SwapExactTokensForTokensMethod -> {
                val firstCoinInPath = contractMethod.path.firstOrNull() ?: return null
                val lastCoinInPath = contractMethod.path.lastOrNull() ?: return null

                val amountOut = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(contractMethod.amountOutMin)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(contractMethod.to, lastCoinInPath, eventInstances, false))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(contractMethod.amountIn),
                    amountOut,
                    findEip20Token(eventInstances, firstCoinInPath),
                    findEip20Token(eventInstances, lastCoinInPath),
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline
                )
            }

            is SwapTokensForExactETHMethod -> {
                val firstCoinInPath = contractMethod.path.firstOrNull() ?: return null

                val amountIn = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(contractMethod.amountInMax)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(contractMethod.to, firstCoinInPath, eventInstances, true))
                }

                return SwapDecoration(
                    to,
                    amountIn,
                    SwapDecoration.Amount.Exact(contractMethod.amountOut),
                    findEip20Token(eventInstances, firstCoinInPath),
                    SwapDecoration.Token.EvmCoin,
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline
                )
            }

            is SwapTokensForExactTokensMethod -> {
                val firstCoinInPath = contractMethod.path.firstOrNull() ?: return null
                val lastCoinInPath = contractMethod.path.lastOrNull() ?: return null

                val amountIn = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(contractMethod.amountInMax)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(contractMethod.to, firstCoinInPath, eventInstances, true))
                }

                return SwapDecoration(
                    to,
                    amountIn,
                    SwapDecoration.Amount.Exact(contractMethod.amountOut),
                    findEip20Token(eventInstances, firstCoinInPath),
                    findEip20Token(eventInstances, lastCoinInPath),
                    if (contractMethod.to == from) null else contractMethod.to,
                    contractMethod.deadline

                )
            }

            else -> return null
        }
    }

    private fun findEip20Token(eventInstances: List<ContractEventInstance>, tokenAddress: Address): SwapDecoration.Token {
        val tokenInfo = eventInstances
            .mapNotNull { it as TransferEventInstance }
            .firstOrNull { it.contractAddress == tokenAddress }?.tokenInfo

        return SwapDecoration.Token.Eip20Coin(tokenAddress, tokenInfo)
    }

    private fun totalTokenAmount(userAddress: Address, tokenAddress: Address, eventInstances: List<ContractEventInstance>, collectIncomingAmounts: Boolean): BigInteger {
        var amountIn: BigInteger = 0.toBigInteger()
        var amountOut: BigInteger = 0.toBigInteger()

        eventInstances.forEach { eventInstance ->
            if (eventInstance.contractAddress == tokenAddress) {
                (eventInstance as? TransferEventInstance)?.let { transferEventDecoration ->
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
