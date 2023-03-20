package io.horizontalsystems.ethereumkit.models

enum class Chain(
        val id: Int,
        val coinType: Int,
        val syncInterval: Long,
        val isEIP1559Supported: Boolean
) {
    Ethereum(1, 60, 15, true),
    BinanceSmartChain(56, 60, 15, false),
    Polygon(137, 60, 15, true),
    Optimism(10, 60, 15, false),
    ArbitrumOne(42161, 60, 15, false),
    Avalanche(43114, 60, 15, true),
    Gnosis(100, 60, 15, true),
    Fantom(250, 60, 15, false),
    EthereumGoerli(5, 1, 15, true);

    val isMainNet = coinType != 1
}
