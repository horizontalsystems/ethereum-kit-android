package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

abstract class OneInchDecoration(
    open val contractAddress: Address
) : TransactionDecoration {

    sealed class Amount(val value: BigInteger) {
        class Exact(value: BigInteger) : Amount(value)
        class Extremum(value: BigInteger) : Amount(value)
    }

    sealed class Token {
        object EvmCoin : Token()
        class Eip20Coin(val address: Address, val tokenInfo: TokenInfo? = null) : Token()

        val info: TokenInfo?
            get() = when (this) {
                is Eip20Coin -> tokenInfo
                EvmCoin -> null
            }
    }

    override fun tags() = listOf(contractAddress.hex, TransactionTag.SWAP)

    protected fun getTags(token: Token, type: String) = when (token) {
        is Token.EvmCoin -> listOf(
            "${TransactionTag.EVM_COIN}_$type",
            TransactionTag.EVM_COIN,
            type
        )

        is Token.Eip20Coin -> listOf("${token.address.hex}_$type", token.address.hex, type)
    }

}
