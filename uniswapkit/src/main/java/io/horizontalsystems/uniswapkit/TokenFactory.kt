package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.uniswapkit.models.Token

class TokenFactory(networkType: EthereumKit.NetworkType) {
    private val wethAddress = wethAddress(networkType)

    fun etherToken(): Token {
        return Token.Ether(wethAddress)
    }

    fun token(contractAddress: ByteArray, decimals: Int): Token {
        return Token.Erc20(contractAddress, decimals)
    }

    companion object {
        private fun wethAddress(networkType: EthereumKit.NetworkType): ByteArray {
            return when (networkType) {
                EthereumKit.NetworkType.MainNet -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
                EthereumKit.NetworkType.Ropsten -> "0xc778417e063141139fce010982780140aa0cd5ab"
                EthereumKit.NetworkType.Kovan -> "0xd0A1E359811322d97991E03f863a0C30C2cF029C"
                EthereumKit.NetworkType.Rinkeby -> "0xc778417e063141139fce010982780140aa0cd5ab"
            }.hexStringToByteArray()
        }
    }

}
