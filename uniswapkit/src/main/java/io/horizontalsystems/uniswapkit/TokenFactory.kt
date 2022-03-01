package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.uniswapkit.models.Token

class TokenFactory(chain: Chain) {
    private val wethAddress = getWethAddress(chain)

    sealed class UnsupportedChainError : Throwable() {
        object NoWethAddress : UnsupportedChainError()
    }

    fun etherToken(): Token {
        return Token.Ether(wethAddress)
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return Token.Erc20(contractAddress, decimals)
    }

    companion object {
        private fun getWethAddress(chain: Chain): Address {
            val wethAddressHex = when (chain.id) {
                1 -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
                56 -> "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c"
                137 -> "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270"
                3, 4 -> "0xc778417E063141139Fce010982780140Aa0cD5Ab"
                42 -> "0xd0A1E359811322d97991E03f863a0C30C2cF029C"
                5 -> "0xB4FBF271143F4FBf7B91A5ded31805e42b2208d6"
                 else -> throw UnsupportedChainError.NoWethAddress
            }
            return Address(wethAddressHex)
        }
    }

}
