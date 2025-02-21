package io.horizontalsystems.uniswapkit

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.uniswapkit.models.Token

class TokenFactory {

    sealed class UnsupportedChainError : Throwable() {
        object NoWethAddress : UnsupportedChainError()
    }

    fun etherToken(chain: Chain): Token {
        return Token.Ether(getWethAddress(chain))
    }

    fun token(contractAddress: Address, decimals: Int): Token {
        return Token.Erc20(contractAddress, decimals)
    }

    companion object {
        fun getWethAddress(chain: Chain): Address {
            val wethAddressHex = when (chain) {
                Chain.Ethereum -> "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2"
                Chain.Optimism -> "0x4200000000000000000000000000000000000006"
                Chain.BinanceSmartChain -> "0xbb4cdb9cbd36b01bd1cbaebf2de08d9173bc095c"
                Chain.Polygon -> "0x0d500B1d8E8eF31E21C99d1Db9A6444d3ADf1270"
                Chain.Avalanche -> "0xB31f66AA3C1e785363F0875A1B74E27b85FD66c7"
                Chain.EthereumGoerli -> "0xB4FBF271143F4FBf7B91A5ded31805e42b2208d6"
                Chain.ArbitrumOne -> "0x82aF49447D8a07e3bd95BD0d56f35241523fBab1"
                Chain.Base -> "0x4200000000000000000000000000000000000006"
                Chain.ZkSync -> "0x5AEa5775959fBC2557Cc8789bC1bf90A239D9a91"
                 else -> throw UnsupportedChainError.NoWethAddress
            }
            return Address(wethAddressHex)
        }
    }

}
