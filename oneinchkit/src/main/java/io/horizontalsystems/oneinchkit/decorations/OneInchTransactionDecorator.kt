package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.oneinchkit.contracts.v4.SwapMethodV4
import io.horizontalsystems.oneinchkit.contracts.v4.UnoswapMethodV4
import io.horizontalsystems.oneinchkit.contracts.v4.UnparsedSwapMethodV4
import io.horizontalsystems.oneinchkit.contracts.v5.SwapMethodV5
import io.horizontalsystems.oneinchkit.contracts.v5.UnoswapMethodV5
import io.horizontalsystems.oneinchkit.contracts.v5.UnparsedSwapMethodV5
import java.math.BigInteger

class OneInchTransactionDecorator(
    private val address: Address
) : ITransactionDecorator {

    private val evmTokenAddresses = mutableListOf("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE", "0x0000000000000000000000000000000000000000")

    override fun decoration(
        from: Address?,
        to: Address?,
        value: BigInteger?,
        contractMethod: ContractMethod?,
        internalTransactions: List<InternalTransaction>,
        eventInstances: List<ContractEventInstance>
    ): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        when (contractMethod) {
            is SwapMethodV4 -> {
                val swapDescription = contractMethod.swapDescription
                val tokenOut = addressToToken(swapDescription.dstToken, eventInstances)
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(swapDescription.minReturnAmount)

                when (tokenOut) {
                    OneInchDecoration.Token.EvmCoin -> {
                        if (internalTransactions.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(getTotalETHIncoming(swapDescription.dstReceiver, internalTransactions))
                        }
                    }

                    is OneInchDecoration.Token.Eip20Coin -> {
                        if (eventInstances.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(
                                getTotalToken(
                                    swapDescription.dstReceiver,
                                    swapDescription.dstToken,
                                    eventInstances,
                                    true
                                )
                            )
                        }
                    }
                }

                return OneInchSwapDecoration(
                    to,
                    addressToToken(swapDescription.srcToken, eventInstances),
                    tokenOut,
                    swapDescription.amount,
                    amountOut,
                    swapDescription.flags,
                    swapDescription.permit,
                    contractMethod.data,
                    if (swapDescription.dstReceiver == from) null else swapDescription.dstReceiver
                )
            }

            is SwapMethodV5 -> {
                val swapDescription = contractMethod.swapDescription
                val tokenOut = addressToToken(swapDescription.dstToken, eventInstances)
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(swapDescription.minReturnAmount)

                when (tokenOut) {
                    OneInchDecoration.Token.EvmCoin -> {
                        if (internalTransactions.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(getTotalETHIncoming(swapDescription.dstReceiver, internalTransactions))
                        }
                    }

                    is OneInchDecoration.Token.Eip20Coin -> {
                        if (eventInstances.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(
                                getTotalToken(
                                    swapDescription.dstReceiver,
                                    swapDescription.dstToken,
                                    eventInstances,
                                    true
                                )
                            )
                        }
                    }
                }

                return OneInchSwapDecoration(
                    to,
                    addressToToken(swapDescription.srcToken, eventInstances),
                    tokenOut,
                    swapDescription.amount,
                    amountOut,
                    swapDescription.flags,
                    contractMethod.permit,
                    contractMethod.data,
                    if (swapDescription.dstReceiver == from) null else swapDescription.dstReceiver
                )
            }

            is UnoswapMethodV4 -> {
                var tokenOut: OneInchDecoration.Token? = null
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(contractMethod.minReturn)

                if (internalTransactions.isNotEmpty()) {
                    val amount = getTotalETHIncoming(address, internalTransactions)

                    if (amount > BigInteger.ZERO) {
                        tokenOut = OneInchDecoration.Token.EvmCoin
                        amountOut = OneInchDecoration.Amount.Exact(amount)
                    }
                }

                if (tokenOut == null && eventInstances.isNotEmpty()) {
                    val tokenAmountOut = getTokenAmount(eventInstances, true)

                    if (tokenAmountOut != null) {
                        tokenOut = tokenAmountOut.first
                        amountOut = OneInchDecoration.Amount.Exact(tokenAmountOut.second)
                    }
                }

                return OneInchUnoswapDecoration(
                    to,
                    addressToToken(contractMethod.srcToken, eventInstances),
                    tokenOut,
                    contractMethod.amount,
                    amountOut,
                    contractMethod.params
                )
            }

            is UnoswapMethodV5 -> {
                var tokenOut: OneInchDecoration.Token? = null
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(contractMethod.minReturn)

                if (internalTransactions.isNotEmpty()) {
                    val amount = getTotalETHIncoming(address, internalTransactions)

                    if (amount > BigInteger.ZERO) {
                        tokenOut = OneInchDecoration.Token.EvmCoin
                        amountOut = OneInchDecoration.Amount.Exact(amount)
                    }
                }

                if (tokenOut == null && eventInstances.isNotEmpty()) {
                    val tokenAmountOut = getTokenAmount(eventInstances, true)

                    if (tokenAmountOut != null) {
                        tokenOut = tokenAmountOut.first
                        amountOut = OneInchDecoration.Amount.Exact(tokenAmountOut.second)
                    }
                }

                return OneInchUnoswapDecoration(
                    to,
                    addressToToken(contractMethod.srcToken, eventInstances),
                    tokenOut,
                    contractMethod.amount,
                    amountOut,
                    Bytes32Array(arrayOf())
                )
            }

            is UnparsedSwapMethodV4, is UnparsedSwapMethodV5 -> {
                val tokenAmountIn: OneInchUnknownDecoration.TokenAmount?
                val tokenAmountOut: OneInchUnknownDecoration.TokenAmount?

                val incomingEth = getTotalETHIncoming(address, internalTransactions)
                val outgoingEth = value - incomingEth
                val incomingTokenAmount = getTokenAmount(eventInstances, true)?.let { OneInchUnknownDecoration.TokenAmount(it.first, it.second) }
                val outgoingTokenAmount = getTokenAmount(eventInstances, false)?.let { OneInchUnknownDecoration.TokenAmount(it.first, it.second) }

                when {
                    outgoingEth > BigInteger.ZERO -> {
                        tokenAmountIn = OneInchUnknownDecoration.TokenAmount(OneInchDecoration.Token.EvmCoin, outgoingEth)
                        tokenAmountOut = incomingTokenAmount
                    }
                    outgoingEth < BigInteger.ZERO -> {
                        tokenAmountIn = outgoingTokenAmount
                        tokenAmountOut = OneInchUnknownDecoration.TokenAmount(OneInchDecoration.Token.EvmCoin, outgoingEth)
                    }
                    else -> {
                        tokenAmountIn = outgoingTokenAmount
                        tokenAmountOut = incomingTokenAmount
                    }
                }

                return OneInchUnknownDecoration(to, tokenAmountIn, tokenAmountOut)
            }

            else -> return null
        }
    }

    private fun findEip20Token(eventInstances: List<ContractEventInstance>, tokenAddress: Address): OneInchDecoration.Token {
        val tokenInfo = eventInstances
            .mapNotNull { it as? TransferEventInstance }
            .firstOrNull { it.contractAddress == tokenAddress }?.tokenInfo

        return OneInchDecoration.Token.Eip20Coin(tokenAddress, tokenInfo)
    }

    private fun getTokenAmount(eventInstances: List<ContractEventInstance>, incoming: Boolean): Pair<OneInchDecoration.Token, BigInteger>? {
        var resolvedToken: OneInchDecoration.Token? = null
        var resolvedAmount: BigInteger? = null

        val eip20EventInstance = eventInstances.firstOrNull { eventInstance ->
            if (eventInstance is TransferEventInstance) {
                return@firstOrNull if (incoming) eventInstance.to == address else eventInstance.from == address
            }

            return@firstOrNull false
        }

        if (eip20EventInstance != null) {
            val amount = getTotalToken(address, eip20EventInstance.contractAddress, eventInstances, incoming)

            if (amount > BigInteger.ZERO) {
                resolvedToken = findEip20Token(eventInstances, eip20EventInstance.contractAddress)
                resolvedAmount = amount
            }
        }

        return if (resolvedToken != null && resolvedAmount != null) {
            Pair(resolvedToken, resolvedAmount)
        } else {
            null
        }
    }

    private fun getTotalToken(
        userAddress: Address,
        tokenAddress: Address,
        eventInstances: List<ContractEventInstance>,
        incoming: Boolean
    ): BigInteger {
        var amountOut = BigInteger.ZERO

        for (eventInstance in eventInstances) {
            if (eventInstance.contractAddress == tokenAddress) {
                (eventInstance as? TransferEventInstance)?.let { transferEventDecoration ->
                    if ((incoming && transferEventDecoration.to == userAddress) || (!incoming && transferEventDecoration.from == userAddress)) {
                        if (transferEventDecoration.value > BigInteger.ZERO) {
                            amountOut += transferEventDecoration.value
                        }
                    }
                }
            }
        }

        return amountOut
    }

    private fun getTotalETHIncoming(userAddress: Address, internalTransactions: List<InternalTransaction>): BigInteger {
        var amountOut = BigInteger.ZERO

        for (internalTransaction in internalTransactions) {
            if (internalTransaction.to == userAddress) {
                amountOut += internalTransaction.value
            }
        }

        return amountOut
    }

    private fun addressToToken(address: Address, eventInstances: List<ContractEventInstance>) = if (evmTokenAddresses.contains(address.eip55))
        OneInchDecoration.Token.EvmCoin
    else
        findEip20Token(eventInstances, address)

}
