package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.ITransactionDecorator
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.oneinchkit.contracts.OneInchV4Method
import io.horizontalsystems.oneinchkit.contracts.SwapMethod
import io.horizontalsystems.oneinchkit.contracts.UnoswapMethod
import java.math.BigInteger

class OneInchTransactionDecorator(
    private val address: Address
) : ITransactionDecorator {

    private val evmTokenAddresses = mutableListOf("0xEeeeeEeeeEeEeeEeEeEeeEEEeeeeEeeeeeeeEEeE", "0x0000000000000000000000000000000000000000")

    override fun decoration(from: Address?, to: Address?, value: BigInteger?, contractMethod: ContractMethod?, internalTransactions: List<InternalTransaction>, eventInstances: List<ContractEventInstance>): TransactionDecoration? {
        if (from == null || to == null || value == null || contractMethod == null) return null

        when (contractMethod) {
            is SwapMethod -> {
                val swapDescription = contractMethod.swapDescription
                val tokenOut = addressToToken(swapDescription.dstToken)
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(swapDescription.minReturnAmount)

                when (tokenOut) {
                    OneInchDecoration.Token.EvmCoin -> {
                        if (internalTransactions.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(totalETHIncoming(swapDescription.dstReceiver, internalTransactions))
                        }
                    }

                    is OneInchDecoration.Token.Eip20Coin -> {
                        if (eventInstances.isNotEmpty()) {
                            amountOut = OneInchDecoration.Amount.Exact(totalTokenIncoming(swapDescription.dstReceiver, swapDescription.dstToken, eventInstances))
                        }

                    }
                }

                return OneInchSwapDecoration(
                    to,
                    addressToToken(swapDescription.srcToken),
                    tokenOut,
                    swapDescription.amount,
                    amountOut,
                    swapDescription.flags,
                    swapDescription.permit,
                    contractMethod.data,
                    if (swapDescription.dstReceiver == from) null else swapDescription.dstReceiver
                )
            }

            is UnoswapMethod -> {
                var tokenOut: OneInchDecoration.Token? = null
                var amountOut: OneInchDecoration.Amount = OneInchDecoration.Amount.Extremum(contractMethod.minReturn)

                if (internalTransactions.isNotEmpty()) {
                    val amount = totalETHIncoming(address, internalTransactions)

                    if (amount > BigInteger.ZERO) {
                        tokenOut = OneInchDecoration.Token.EvmCoin
                        amountOut = OneInchDecoration.Amount.Exact(amount)
                    }
                }

                if (tokenOut == null && eventInstances.isNotEmpty()) {
                    val incomingEip20EventInstance = eventInstances.firstOrNull { eventInstance ->
                        if (eventInstance is TransferEventInstance) {
                            return@firstOrNull eventInstance.to == address
                        }

                        return@firstOrNull false
                    }

                    if (incomingEip20EventInstance != null) {
                        val amount = totalTokenIncoming(address, incomingEip20EventInstance.contractAddress, eventInstances)

                        if (amount > BigInteger.ZERO) {
                            tokenOut = OneInchDecoration.Token.Eip20Coin(incomingEip20EventInstance.contractAddress)
                            amountOut = OneInchDecoration.Amount.Exact(amount)
                        }
                    }
                }

                return OneInchUnoswapDecoration(
                    to,
                    addressToToken(contractMethod.srcToken),
                    tokenOut,
                    contractMethod.amount,
                    amountOut,
                    contractMethod.params
                )
            }

            is OneInchV4Method -> {
                return OneInchUnknownDecoration(to, address, value, internalTransactions, eventInstances)
            }

            else -> return null
        }
    }

    private fun totalTokenIncoming(userAddress: Address, tokenAddress: Address, eventInstances: List<ContractEventInstance>): BigInteger {
        var amountOut = BigInteger.ZERO

        for (eventInstance in eventInstances) {
            if (eventInstance.contractAddress == tokenAddress) {
                (eventInstance as? TransferEventInstance)?.let { transferEventDecoration ->
                    if (transferEventDecoration.to == userAddress && transferEventDecoration.value > BigInteger.ZERO) {
                        amountOut += transferEventDecoration.value
                    }
                }
            }
        }

        return amountOut
    }

    private fun totalETHIncoming(userAddress: Address, internalTransactions: List<InternalTransaction>): BigInteger {
        var amountOut = BigInteger.ZERO

        for (internalTransaction in internalTransactions) {
            if (internalTransaction.to == userAddress) {
                amountOut += internalTransaction.value
            }
        }

        return amountOut
    }

    private fun addressToToken(address: Address) = if (evmTokenAddresses.contains(address.eip55))
        OneInchDecoration.Token.EvmCoin
    else
        OneInchDecoration.Token.Eip20Coin(address)

}
