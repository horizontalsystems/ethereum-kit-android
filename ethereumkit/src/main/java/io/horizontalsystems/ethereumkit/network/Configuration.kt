package io.horizontalsystems.ethereumkit.network

class Configuration(val networkType: NetworkType, val infuraKey: String, val etherscanAPIKey: String, val debugPrints: Boolean) {
    val etherScanUrl: String
        get() {
            return when (networkType) {
                NetworkType.MainNet -> "https://api.etherscan.io"
                NetworkType.Ropsten -> "https://api-ropsten.etherscan.io"
                NetworkType.Kovan -> "https://api-kovan.etherscan.io"
                NetworkType.Rinkeby -> "https://api-rinkeby.etherscan.io"
            }
        }

    private val subDomain = when (networkType) {
        NetworkType.MainNet -> "mainnet"
        NetworkType.Kovan -> "kovan"
        NetworkType.Rinkeby -> "rinkeby"
        NetworkType.Ropsten -> "ropsten"
    }

    val infuraUrl: String = "https://$subDomain.infura.io/$infuraKey"
}
