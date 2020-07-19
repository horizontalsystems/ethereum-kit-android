package io.horizontalsystems.uniswapkit.models

import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.core.toRawHexString
import java.util.*

sealed class Token(
        val address: ByteArray,
        val decimals: Int,
        val isEther: Boolean = false) {

    class Ether(wethAddress: ByteArray) : Token(wethAddress, 18, true)
    class Erc20(address: ByteArray, decimals: Int) : Token(address, decimals)

    fun sortsBefore(token: Token): Boolean {
        return address.toRawHexString() < token.address.toRawHexString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Token)
            return false

        return isEther == other.isEther && address.contentEquals(other.address)
    }

    override fun hashCode(): Int {
        return Objects.hash(isEther, address.contentHashCode())
    }

    override fun toString(): String {
        return when (this) {
            is Ether -> "Ether"
            is Erc20 -> "Erc20"
        }
    }
}
