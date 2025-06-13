package io.horizontalsystems.ethereumkit.models

enum class Chain(
    val id: Int,
    val coinType: Int,
    val gasLimit: Long,
    val syncInterval: Long,
    val isEIP1559Supported: Boolean
) {
    Ethereum(1, 60, 2_000_000, 15, true),
    BinanceSmartChain(56, 60, 10_000_000, 15, false),
    Base(8453, 60, 20_000_000, 15, true),
    ZkSync(324, 60, 10_000_000, 15, true),
    Polygon(137, 60, 10_000_000, 15, true),
    Optimism(10, 60, 10_000_000, 15, false),
    ArbitrumOne(42161, 60, 10_000_000, 15, false),
    Avalanche(43114, 60, 10_000_000, 15, true),
    Gnosis(100, 60, 10_000_000, 15, true),
    Fantom(250, 60, 10_000_000, 15, false),
    EthereumGoerli(5, 1, 10_000_000, 15, true);

    val isMainNet = coinType != 1
}
