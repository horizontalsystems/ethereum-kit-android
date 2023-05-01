package io.horizontalsystems.uniswapkit.v3

import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import io.horizontalsystems.uniswapkit.v3.router.*
import java.math.BigInteger

class UniswapV3TransactionDecorator(private val wethAddress: Address) : ITransactionDecorator {

    override fun decoration(from: Address?, to: Address?, value: BigInteger?, contractMethod: ContractMethod?, internalTransactions: List<InternalTransaction>, eventInstances: List<ContractEventInstance>): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        when (contractMethod) {
            is ExactInputSingleMethod -> {
                return exactIn(
                    from = from,
                    to = to,
                    value = value,
                    eventInstances = eventInstances,
                    tokenIn = contractMethod.tokenIn,
                    tokenOut = contractMethod.tokenOut,
                    amountIn = contractMethod.amountIn,
                    amountOutMinimum = contractMethod.amountOutMinimum,
                    recipient = contractMethod.recipient,
                )
            }
            is ExactInputMethod -> {
                return exactIn(
                    from = from,
                    to = to,
                    value = value,
                    eventInstances = eventInstances,
                    tokenIn = contractMethod.tokenIn,
                    tokenOut = contractMethod.tokenOut,
                    amountIn = contractMethod.amountIn,
                    amountOutMinimum = contractMethod.amountOutMinimum,
                    recipient = contractMethod.recipient,
                )
            }
            is ExactOutputSingleMethod -> {
                return swapDecoration(
                    from,
                    to,
                    value,
                    eventInstances,
                    contractMethod.tokenIn,
                    contractMethod.tokenOut,
                    contractMethod.amountOut,
                    contractMethod.amountInMaximum,
                    contractMethod.recipient,
                )
            }
            is ExactOutputMethod -> {
                return swapDecoration(
                    from,
                    to,
                    value,
                    eventInstances,
                    contractMethod.tokenIn,
                    contractMethod.tokenOut,
                    contractMethod.amountOut,
                    contractMethod.amountInMaximum,
                    contractMethod.recipient,
                )
            }

//            is MulticallMethod -> {
//                val tokenOutIsEther = tokenOut is Ether
//
//                when {
//                    tokenOutIsEther -> {
//                        val amountOut = if (internalTransactions.isEmpty()) {
//                            SwapDecoration.Amount.Extremum(contractMethod.amountOutMinimum)
//                        } else {
//                            SwapDecoration.Amount.Exact(totalETHIncoming(contractMethod.recipient, internalTransactions))
//                        }
//
//                        return SwapDecoration(
//                            to,
//                            SwapDecoration.Amount.Exact(contractMethod.amountIn),
//                            amountOut,
//                            findEip20Token(eventInstances, tokenIn),
//                            SwapDecoration.Token.EvmCoin,
//                            if (contractMethod.recipient == from) null else contractMethod.recipient,
//                            contractMethod.deadline
//                        )
//                    }
//                }
//            }

            else -> return null
        }
    }

    private fun swapDecoration(
        from: Address,
        to: Address,
        value: BigInteger,
        eventInstances: List<ContractEventInstance>,
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger,
        amountInMaximum: BigInteger,
        recipient: Address,
    ): SwapDecoration {
        val amountIn = if (eventInstances.isEmpty()) {
            SwapDecoration.Amount.Extremum(amountInMaximum)
        } else {
            SwapDecoration.Amount.Exact(totalTokenAmount(recipient, tokenIn, eventInstances, true))
        }

        val tokenInIsEther = value > BigInteger.ZERO && tokenIn == wethAddress
        val swapDecorationTokenIn = if (tokenInIsEther) {
            SwapDecoration.Token.EvmCoin
        } else {
            findEip20Token(eventInstances, tokenOut)
        }

        return SwapDecoration(
            contractAddress = to,
            amountIn = amountIn,
            amountOut = SwapDecoration.Amount.Exact(amountOut),
            tokenIn = swapDecorationTokenIn,
            tokenOut = findEip20Token(eventInstances, tokenOut),
            recipient = if (recipient == from) null else recipient,
            deadline = null
        )
    }

    private fun exactIn(
        from: Address,
        to: Address,
        value: BigInteger,
        eventInstances: List<ContractEventInstance>,
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger,
        amountOutMinimum: BigInteger,
        recipient: Address,
    ): SwapDecoration {
        val amountOut = if (eventInstances.isEmpty()) {
            SwapDecoration.Amount.Extremum(amountOutMinimum)
        } else {
            SwapDecoration.Amount.Exact(
                totalTokenAmount(
                    recipient,
                    tokenOut,
                    eventInstances,
                    false
                )
            )
        }

        val swapDecorationAmountIn: SwapDecoration.Amount.Exact
        val swapDecorationTokenIn: SwapDecoration.Token

        val tokenInIsEther = value > BigInteger.ZERO && tokenIn == wethAddress
        if (tokenInIsEther) {
            swapDecorationAmountIn = SwapDecoration.Amount.Exact(value)
            swapDecorationTokenIn = SwapDecoration.Token.EvmCoin
        } else {
            swapDecorationAmountIn = SwapDecoration.Amount.Exact(amountIn)
            swapDecorationTokenIn = findEip20Token(eventInstances, tokenIn)
        }

        return SwapDecoration(
            contractAddress = to,
            amountIn = swapDecorationAmountIn,
            amountOut = amountOut,
            tokenIn = swapDecorationTokenIn,
            tokenOut = findEip20Token(eventInstances, tokenOut),
            recipient = if (recipient == from) null else recipient,
            deadline = null
        )
    }

    private fun findEip20Token(eventInstances: List<ContractEventInstance>, tokenAddress: Address): SwapDecoration.Token {
        val tokenInfo = eventInstances
            .mapNotNull { it as? TransferEventInstance }
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