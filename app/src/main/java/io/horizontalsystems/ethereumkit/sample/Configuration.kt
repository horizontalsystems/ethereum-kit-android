package io.horizontalsystems.ethereumkit.sample

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.sample.modules.main.Erc20Token

object Configuration {
    const val webSocket: Boolean = false
    val chain: Chain = Chain.EthereumGoerli
    const val walletId = "walletId"
    val watchAddress: String? = null
    const val defaultsWords = "apart approve black comfort steel spin real renew tone primary key cherry"

    const val infuraProjectId = "2a1306f1d12f4c109a4d4fb9be46b02e"
    const val infuraSecret = "fc479a9290b64a84a15fa6544a130218"
    const val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    const val bscScanKey = "5ZGSHWYHZVA8XZHB8PF6UUTRNNB4KT43ZZ"

    val erc20Tokens: List<Erc20Token>
        get() {
            return when (chain) {
                Chain.Ethereum -> listOf(
                        Erc20Token("DAI", "DAI", Address("0x6b175474e89094c44da98b954eedeac495271d0f"), 18),
                        Erc20Token("USD Coin", "USDC", Address("0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"), 6)
                )
                Chain.BinanceSmartChain -> listOf(
                        Erc20Token("Beefy.Finance", "BIFI", Address("0xCa3F508B8e4Dd382eE878A314789373D80A5190A"), 18),
                        Erc20Token("PancakeSwap", "CAKE", Address("0x0e09fabb73bd3ade0a17ecc321fd13a19e81ce82"), 18),
                        Erc20Token("BUSD", "BUSD", Address("0xe9e7cea3dedca5984780bafc599bd69add087d56"), 18)
                )
                Chain.EthereumGoerli -> listOf(
                    Erc20Token("WEENUS", "WEENUS", Address("0xaff4481d10270f50f203e0763e2597776068cbc5"), 18),
                    Erc20Token("USDT", "USDT", Address("0x183F3D42f1F78498f16bC6de7F5A6328fE39f25c"), 6)
                )
                else -> listOf()
            }
        }
}
