package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.models.Token

class TokenFactory(networkType: EthereumKit.NetworkType) {
    private val wethAddress = wethAddress(networkType)

    fun etherToken(): Token {
        return Token.Ether(wethAddress)
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return Token.Erc20(contractAddress, decimals)
    }

    companion object {
        private fun wethAddress(networkType: EthereumKit.NetworkType): Address {
            val wethAddressHex = when (networkType) {
                EthereumKit.NetworkType.EthMainNet -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
                EthereumKit.NetworkType.EthRopsten -> "0xc778417e063141139fce010982780140aa0cd5ab"
                EthereumKit.NetworkType.EthKovan -> "0xd0A1E359811322d97991E03f863a0C30C2cF029C"
                else -> "0xc778417e063141139fce010982780140aa0cd5ab"
            }
            return Address(wethAddressHex)
        }
    }

}
