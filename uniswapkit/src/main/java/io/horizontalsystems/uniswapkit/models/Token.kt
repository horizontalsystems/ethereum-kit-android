package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.models.Address
import java.util.*

sealed class Token(
        val address: Address,
        val decimals: Int,
        val isEther: Boolean = false) {

    class Ether(wethAddress: Address) : Token(wethAddress, 18, true)
    class Erc20(address: Address, decimals: Int) : Token(address, decimals)

    fun sortsBefore(token: Token): Boolean {
        return address.hex < token.address.hex
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Token)
            return false

        return decimals == other.decimals && isEther == other.isEther && address == other.address
    }

    override fun hashCode(): Int {
        return Objects.hash(isEther, decimals, address.hashCode())
    }

    override fun toString(): String {
        return when (this) {
            is Ether -> "Ether"
            is Erc20 -> "Erc20"
        }
    }
}
