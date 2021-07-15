package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class OneInchUnoswapMethodDecoration(
        val fromToken: Token,
        val toToken: Token?,
        val fromAmount: BigInteger,
        val toAmount: BigInteger,
        val params: Bytes32Array
) : OneInchMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(toAddress.hex, "swap")

        when (fromToken) {
            Token.EvmCoin -> {
                tags.addAll(listOf("ETH_outgoing", "ETH", "outgoing"))
            }
            is Token.Eip20 -> {
                val addressHex = fromToken.address.hex
                tags.addAll(listOf("${addressHex}_outgoing", addressHex, "outgoing"))
            }
        }

        toToken?.let {
            when (toToken) {
                Token.EvmCoin -> {
                    tags.addAll(listOf("ETH_incoming", "ETH", "incoming"))
                }
                is Token.Eip20 -> {
                    val addressHex = toToken.address.hex
                    tags.addAll(listOf("${addressHex}_incoming", addressHex, "incoming"))
                }
            }
        }

        return tags
    }

}
