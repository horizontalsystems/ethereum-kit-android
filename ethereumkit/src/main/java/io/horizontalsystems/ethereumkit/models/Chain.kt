package io.horizontalsystems.ethereumkit.models

sealed class Chain(
    val id: Int,
    val coinType: Int,
    val blockTime: Long,
    val isEIP1559Supported: Boolean
) {
    val isMainNet = coinType != 1

    object Ethereum: Chain(1, 60, 15, true)
    object BinanceSmartChain: Chain(56, 60, 5, false)
    object Polygon: Chain(137, 60, 1, true)
    object EthereumRopsten: Chain(3, 1, 15, true)
    object EthereumKovan: Chain(42, 1, 4, true)
    object EthereumRinkeby: Chain(4, 1, 15, true)
    object EthereumGoerli: Chain(5, 1, 15, true)

}
