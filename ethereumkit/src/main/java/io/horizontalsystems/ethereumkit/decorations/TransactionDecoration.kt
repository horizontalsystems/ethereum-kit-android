package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

sealed class TransactionDecoration {
    class Unknown(val methodId: ByteArray, val inputArguments: ByteArray) : TransactionDecoration()
    class Recognized(val method: String, val arguments: List<Any>) : TransactionDecoration()
    class Eip20Transfer(val to: Address, val value: BigInteger) : TransactionDecoration()
    class Eip20Approve(val spender: Address, val value: BigInteger) : TransactionDecoration()
    class Swap(val trade: Trade, val tokenIn: Token, val tokenOut: Token, val to: Address, val deadline: BigInteger) : TransactionDecoration() {

        sealed class Trade {
            class ExactIn(val amountIn: BigInteger, val amountOutMin: BigInteger, val amountOut: BigInteger? = null) : Trade()
            class ExactOut(val amountOut: BigInteger, val amountInMax: BigInteger, val amountIn: BigInteger? = null) : Trade()
        }

        sealed class Token {
            object EvmCoin : Token()
            class Eip20Coin(val address: Address) : Token()
        }
    }

    val name: String?
        get() {
            return when (this) {
                is Unknown -> null
                is Recognized -> this.method
                is Eip20Transfer -> "eip20Transfer"
                is Eip20Approve -> "eip20Approve"
                is Swap -> "swap"
            }
        }

}
