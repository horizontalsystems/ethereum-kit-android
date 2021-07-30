package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OneInchUnoswapMethodDecoration(
        val fromToken: Token,
        val toToken: Token?,
        val fromAmount: BigInteger,
        val toAmountMin: BigInteger,
        val toAmount: BigInteger?,
        val params: Bytes32Array
) : OneInchMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(toAddress.hex, TransactionTag.SWAP)

        when (fromToken) {
            Token.EvmCoin -> {
                tags.addAll(listOf(TransactionTag.EVM_COIN_OUTGOING, TransactionTag.EVM_COIN, TransactionTag.OUTGOING))
            }
            is Token.Eip20 -> {
                val addressHex = fromToken.address.hex
                tags.addAll(listOf(TransactionTag.eip20Outgoing(addressHex), addressHex, TransactionTag.OUTGOING))
            }
        }

        toToken?.let {
            when (toToken) {
                Token.EvmCoin -> {
                    tags.addAll(listOf(TransactionTag.EVM_COIN_INCOMING, TransactionTag.EVM_COIN, TransactionTag.INCOMING))
                }
                is Token.Eip20 -> {
                    val addressHex = toToken.address.hex
                    tags.addAll(listOf(TransactionTag.eip20Incoming(addressHex), addressHex, TransactionTag.INCOMING))
                }
            }
        }

        return tags
    }

}
