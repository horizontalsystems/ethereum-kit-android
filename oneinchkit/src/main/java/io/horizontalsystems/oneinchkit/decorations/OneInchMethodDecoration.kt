package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address

abstract class OneInchMethodDecoration: ContractMethodDecoration() {

    sealed class Token {
        object EvmCoin : Token()
        class Eip20(val address: Address) : Token()

        override fun toString(): String {
            return when(this) {
                EvmCoin -> "EvmCoin"
                is Eip20 -> "Eip20($address)"
            }
        }
    }

}
