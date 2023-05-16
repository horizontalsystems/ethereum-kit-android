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

    override fun decoration(
        from: Address?,
        to: Address?,
        value: BigInteger?,
        contractMethod: ContractMethod?,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>
    ): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        var tokenOutIsEther = false
        var recipientOverride: Address? = null
        var swapMethod = contractMethod

        if (contractMethod is MulticallMethod) {
            val unwrapWETH9Method = contractMethod.methods.lastOrNull() as? UnwrapWETH9Method
            swapMethod = contractMethod.methods.firstOrNull {
                it is ExactInputSingleMethod ||
                it is ExactInputMethod ||
                it is ExactOutputSingleMethod ||
                it is ExactOutputMethod
            }
            tokenOutIsEther = unwrapWETH9Method != null
            recipientOverride = unwrapWETH9Method?.recipient
        }

        if (swapMethod == null) return null

        return decoration(from, to, value, swapMethod, internalTransactions, eventInstances, tokenOutIsEther, recipientOverride)
    }

    private fun decoration(
        from: Address,
        to: Address,
        value: BigInteger,
        contractMethod: ContractMethod,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>,
        tokenOutIsEther: Boolean,
        recipientOverride: Address?
    ) = when (contractMethod) {
        is ExactInputSingleMethod -> exactIn(
            from = from,
            to = to,
            value = value,
            internalTransactions = internalTransactions,
            eventInstances = eventInstances,
            tokenIn = contractMethod.tokenIn,
            tokenOut = contractMethod.tokenOut,
            amountIn = contractMethod.amountIn,
            amountOutMinimum = contractMethod.amountOutMinimum,
            recipient = recipientOverride ?: contractMethod.recipient,
            tokenOutIsEther = tokenOutIsEther
        )
        is ExactInputMethod -> exactIn(
            from = from,
            to = to,
            value = value,
            internalTransactions = internalTransactions,
            eventInstances = eventInstances,
            tokenIn = contractMethod.tokenIn,
            tokenOut = contractMethod.tokenOut,
            amountIn = contractMethod.amountIn,
            amountOutMinimum = contractMethod.amountOutMinimum,
            recipient = recipientOverride ?: contractMethod.recipient,
            tokenOutIsEther = tokenOutIsEther
        )
        is ExactOutputSingleMethod -> exactOut(
            from = from,
            to = to,
            value = value,
            internalTransactions = internalTransactions,
            eventInstances = eventInstances,
            tokenIn = contractMethod.tokenIn,
            tokenOut = contractMethod.tokenOut,
            amountOut = contractMethod.amountOut,
            amountInMaximum = contractMethod.amountInMaximum,
            recipient = recipientOverride ?: contractMethod.recipient,
            tokenOutIsEther = tokenOutIsEther
        )
        is ExactOutputMethod -> exactOut(
            from = from,
            to = to,
            value = value,
            internalTransactions = internalTransactions,
            eventInstances = eventInstances,
            tokenIn = contractMethod.tokenIn,
            tokenOut = contractMethod.tokenOut,
            amountOut = contractMethod.amountOut,
            amountInMaximum = contractMethod.amountInMaximum,
            recipient = recipientOverride ?: contractMethod.recipient,
            tokenOutIsEther = tokenOutIsEther
        )
        else -> null
    }

    private fun exactOut(
        from: Address,
        to: Address,
        value: BigInteger,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>,
        tokenIn: Address,
        tokenOut: Address,
        amountOut: BigInteger,
        amountInMaximum: BigInteger,
        recipient: Address,
        tokenOutIsEther: Boolean,
    ): SwapDecoration {
        val swapType = when {
            tokenOutIsEther -> SwapType.TokenToEth
            value > BigInteger.ZERO && tokenIn == wethAddress -> SwapType.EthToToken
            else -> SwapType.TokenToToken
        }

        when (swapType) {
            SwapType.EthToToken -> {
                val amountIn = if (internalTransactions.isEmpty()) {
                    SwapDecoration.Amount.Extremum(value)
                } else {
                    val change = totalETHIncoming(recipient, internalTransactions)
                    SwapDecoration.Amount.Exact(value - change)
                }

                return SwapDecoration(
                    contractAddress = to,
                    amountIn = amountIn,
                    amountOut = SwapDecoration.Amount.Exact(amountOut),
                    tokenIn = SwapDecoration.Token.EvmCoin,
                    tokenOut = findEip20Token(eventInstances, tokenOut),
                    recipient = if (recipient == from) null else recipient,
                    deadline = null
                )
            }
            SwapType.TokenToEth -> {
                val amountIn = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(amountInMaximum)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(recipient, tokenIn, eventInstances, true))
                }

                return SwapDecoration(
                    contractAddress = to,
                    amountIn = amountIn,
                    amountOut = SwapDecoration.Amount.Exact(amountOut),
                    tokenIn = findEip20Token(eventInstances, tokenIn),
                    tokenOut = SwapDecoration.Token.EvmCoin,
                    recipient = if (recipient == from) null else recipient,
                    deadline = null
                )
            }
            SwapType.TokenToToken -> {
                val amountIn = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(amountInMaximum)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(recipient, tokenIn, eventInstances, true))
                }

                return SwapDecoration(
                    to,
                    amountIn,
                    SwapDecoration.Amount.Exact(amountOut),
                    findEip20Token(eventInstances, tokenIn),
                    findEip20Token(eventInstances, tokenOut),
                    if (recipient == from) null else recipient,
                    null
                )
            }
        }
    }

    private fun exactIn(
        from: Address,
        to: Address,
        value: BigInteger,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>,
        tokenIn: Address,
        tokenOut: Address,
        amountIn: BigInteger,
        amountOutMinimum: BigInteger,
        recipient: Address,
        tokenOutIsEther: Boolean,
    ): SwapDecoration {
        val swapType = when {
            tokenOutIsEther -> SwapType.TokenToEth
            value > BigInteger.ZERO && tokenIn == wethAddress -> SwapType.EthToToken
            else -> SwapType.TokenToToken
        }

        when (swapType) {
            SwapType.EthToToken -> {
                val amountOut = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(amountOutMinimum)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(recipient, tokenOut, eventInstances, false))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(value),
                    amountOut,
                    SwapDecoration.Token.EvmCoin,
                    findEip20Token(eventInstances, tokenOut),
                    if (recipient == from) null else recipient,
                    null
                )
            }
            SwapType.TokenToEth -> {
                val amountOut = if (internalTransactions.isEmpty()) {
                    SwapDecoration.Amount.Extremum(amountOutMinimum)
                } else {
                    SwapDecoration.Amount.Exact(totalETHIncoming(recipient, internalTransactions))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(amountIn),
                    amountOut,
                    findEip20Token(eventInstances, tokenIn),
                    SwapDecoration.Token.EvmCoin,
                    if (recipient == from) null else recipient,
                    null
                )
            }
            SwapType.TokenToToken -> {
                val amountOut = if (eventInstances.isEmpty()) {
                    SwapDecoration.Amount.Extremum(amountOutMinimum)
                } else {
                    SwapDecoration.Amount.Exact(totalTokenAmount(recipient, tokenOut, eventInstances, false))
                }

                return SwapDecoration(
                    to,
                    SwapDecoration.Amount.Exact(amountIn),
                    amountOut,
                    findEip20Token(eventInstances, tokenIn),
                    findEip20Token(eventInstances, tokenOut),
                    if (recipient == from) null else recipient,
                    null
                )
            }
        }
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

enum class SwapType {
    EthToToken, TokenToEth, TokenToToken
}