package io.horizontalsystems.ethereumkit.models

class TransactionSource(val name: String, val type: SourceType) {

    fun transactionUrl(hash: String) = "${type.txBaseUrl}/tx/$hash"

    sealed class SourceType {
        abstract val apiBaseUrl: String
        abstract val txBaseUrl: String
        abstract val apiKeys: List<String>

        class Etherscan(
            override val apiBaseUrl: String,
            override val txBaseUrl: String,
            override val apiKeys: List<String>,
        ) : SourceType()
    }

    companion object {
        private fun etherscan(name: String, explorerUrl: String, apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                name, SourceType.Etherscan("https://api.etherscan.io/v2/", explorerUrl, apiKeys)
            )
        }

        // Blockscout PRO API: an Etherscan V2-compatible multichain endpoint for chains that
        // Etherscan does not index. It takes the same `chainid` parameter and requires a
        // `proapi_` key. Public Blockscout instances also expose an Etherscan-like /api, but
        // they hard-throttle it, so it is not usable for syncing.
        private fun blockscoutPro(name: String, explorerUrl: String, apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                name, SourceType.Etherscan("https://api.blockscout.com/v2/", explorerUrl, apiKeys)
            )
        }

        fun ethereum(apiKeys: List<String>): TransactionSource {
            return etherscan("etherscan.io", "https://etherscan.io", apiKeys)
        }

        fun binance(apiKeys: List<String>): TransactionSource {
            return etherscan("bscscan.com", "https://bscscan.com", apiKeys)
        }

        fun polygon(apiKeys: List<String>): TransactionSource {
            return etherscan("polygonscan.com", "https://polygonscan.com", apiKeys)
        }

        fun optimism(apiKeys: List<String>): TransactionSource {
            return etherscan("optimistic.etherscan.io", "https://optimistic.etherscan.io", apiKeys)
        }

        fun arbitrumOne(apiKeys: List<String>): TransactionSource {
            return etherscan("arbiscan.io", "https://arbiscan.io", apiKeys)
        }

        fun avalanche(apiKeys: List<String>): TransactionSource {
            return etherscan("snowtrace.io", "https://snowtrace.io", apiKeys)
        }

        fun gnosis(apiKeys: List<String>): TransactionSource {
            return etherscan("gnosisscan.io", "https://gnosisscan.io", apiKeys)
        }

        fun base(apiKeys: List<String>): TransactionSource {
            return etherscan("basescan.org", "https://basescan.org", apiKeys)
        }

        fun fantom(apiKeys: List<String>): TransactionSource {
            return etherscan("ftmscan.com", "https://ftmscan.com", apiKeys)
        }

        // ZkSync Era is not supported by the Etherscan V2 multichain API, and the old
        // Etherscan-family explorer (era.zksync.network) was shut down.
        fun zkSync(apiKeys: List<String>): TransactionSource {
            return blockscoutPro("zksync.blockscout.com", "https://zksync.blockscout.com", apiKeys)
        }

        // Robinhood Chain is an Arbitrum Orbit L2 not indexed by Etherscan.
        fun robinhood(apiKeys: List<String>): TransactionSource {
            return blockscoutPro("robinhoodchain.blockscout.com", "https://robinhoodchain.blockscout.com", apiKeys)
        }

        fun arc(apiKeys: List<String>): TransactionSource {
            return etherscan("arc.etherscan.io", "https://arc.etherscan.io", apiKeys)
        }
    }

}
