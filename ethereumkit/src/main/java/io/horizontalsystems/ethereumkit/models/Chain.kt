package io.horizontalsystems.ethereumkit.models

data class Chain(
    val id: Int,
    val coinType: Int,
    val blockTime: Long,
    val isEIP1559Supported: Boolean
) {
    val isMainNet get() = coinType != 1

    companion object {
        val ethereum = Chain(1, 60, 15, true)
        val binanceSmartChain = Chain(56, 60, 5, false)
        val polygon = Chain(137, 60, 1, true)
        val ethereumRopsten = Chain(3, 1, 15, true)
        val ethereumKovan = Chain(42, 1, 4, true)
        val ethereumRinkeby = Chain(4, 1, 15, true)
        val ethereumGoerli = Chain(5, 1, 15, true)
    }

}
