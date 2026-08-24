package io.horizontalsystems.ethereumkit.models

class TransactionSource(val name: String, val type: SourceType) {

    fun transactionUrl(hash: String) =
        when (type) {
            is SourceType.Etherscan -> "${type.txBaseUrl}/tx/$hash"
            is SourceType.Blockscout -> "${type.txBaseUrl}/tx/$hash"
        }

    sealed class SourceType {
        class Etherscan(val apiBaseUrl: String, val txBaseUrl: String, val apiKeys: List<String>) : SourceType()

        // Blockscout's modern REST API (/api/v2). Used for chains whose Blockscout instance
        // throttles the legacy Etherscan-compatible /api endpoint for anonymous callers.
        class Blockscout(val apiBaseUrl: String, val txBaseUrl: String, val apiKeys: List<String>) : SourceType()
    }

    companion object {
        private fun etherscan(name: String, explorerUrl: String, apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                name, SourceType.Etherscan("https://api.etherscan.io/v2/", explorerUrl, apiKeys)
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

        fun zkSync(apiKeys: List<String>): TransactionSource {
            return etherscan("era.zksync.network", "https://era.zksync.network", apiKeys)
        }

        // Robinhood Chain is an Arbitrum Orbit L2 not indexed by Etherscan; it exposes a
        // Blockscout instance. Its legacy Etherscan-compatible /api endpoint hard-throttles
        // anonymous callers (HTTP 429 "Too many requests"), so transaction history never loads
        // there. The modern /api/v2 REST endpoint is not throttled, so use that instead.
        fun robinhood(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "robinhoodchain.blockscout.com",
                SourceType.Blockscout(
                    apiBaseUrl = "https://robinhoodchain.blockscout.com/",
                    txBaseUrl = "https://robinhoodchain.blockscout.com",
                    apiKeys = apiKeys
                )
            )
        }

    }

}
