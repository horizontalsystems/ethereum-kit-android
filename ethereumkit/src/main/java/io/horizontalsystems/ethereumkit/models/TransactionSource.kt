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

        // Blockscout's modern REST API (/api/v2). Used for chains that are not indexed by
        // Etherscan or whose Blockscout instance throttles the legacy Etherscan-compatible
        // /api endpoint for anonymous callers.
        class Blockscout(
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

        // A public Blockscout instance serves both the explorer UI and the /api/v2 REST API
        // from the same host, so a single hostname is enough to describe the source.
        private fun blockscout(host: String, apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                host, SourceType.Blockscout("https://$host/", "https://$host", apiKeys)
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
            return blockscout("zksync.blockscout.com", apiKeys)
        }

        // Robinhood Chain is an Arbitrum Orbit L2 not indexed by Etherscan. Its Blockscout
        // instance hard-throttles anonymous callers of the legacy /api endpoint (HTTP 429
        // "Too many requests"), so only the /api/v2 REST endpoint is usable.
        fun robinhood(apiKeys: List<String>): TransactionSource {
            return blockscout("robinhoodchain.blockscout.com", apiKeys)
        }
    }

}
