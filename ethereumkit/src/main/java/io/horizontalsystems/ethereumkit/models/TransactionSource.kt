package io.horizontalsystems.ethereumkit.models

class TransactionSource(val name: String, val type: SourceType) {

    fun transactionUrl(hash: String) =
        when (type) {
            is SourceType.Etherscan -> "${type.txBaseUrl}/tx/$hash"
        }

    sealed class SourceType {
        class Etherscan(val apiBaseUrl: String, val txBaseUrl: String, val apiKeys: List<String>) : SourceType()
    }

    companion object {
        private fun etherscan(apiSubdomain: String, txSubdomain: String?, apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "etherscan.io",
                SourceType.Etherscan("https://$apiSubdomain.etherscan.io/v2/", "https://${txSubdomain?.let { "$it." } ?: ""}etherscan.io", apiKeys)
            )
        }

        fun ethereumEtherscan(apiKeys: List<String>): TransactionSource {
            return etherscan("api", null, apiKeys)
        }

        fun goerliEtherscan(apiKeys: List<String>): TransactionSource {
            return etherscan("api-goerli", "goerli", apiKeys)
        }

        fun bscscan(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "bscscan.com",
                SourceType.Etherscan("https://api.bscscan.com", "https://bscscan.com", apiKeys)
            )
        }

        fun polygonscan(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "polygonscan.com",
                SourceType.Etherscan("https://api.polygonscan.com", "https://polygonscan.com", apiKeys)
            )
        }

        fun optimisticEtherscan(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "optimistic.etherscan.io",
                SourceType.Etherscan("https://api-optimistic.etherscan.io", "https://optimistic.etherscan.io", apiKeys)
            )
        }

        fun arbiscan(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "arbiscan.io",
                SourceType.Etherscan("https://api.arbiscan.io", "https://arbiscan.io", apiKeys)
            )
        }

        fun snowtrace(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "snowtrace.io",
                SourceType.Etherscan("https://api.snowtrace.io", "https://snowtrace.io", apiKeys)
            )
        }

        fun gnosis(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "gnosisscan.io",
                SourceType.Etherscan("https://api.gnosisscan.io", "https://gnosisscan.io", apiKeys)
            )
        }

        fun fantom(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "ftmscan.com",
                SourceType.Etherscan("https://api.ftmscan.com", "https://ftmscan.com", apiKeys)
            )
        }

        fun basescan(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "basescan.org",
                SourceType.Etherscan("https://api.basescan.org", "https://basescan.org", apiKeys)
            )
        }

        fun eraZkSync(apiKeys: List<String>): TransactionSource {
            return TransactionSource(
                "era.zksync.network",
                SourceType.Etherscan("https://api-era.zksync.network", "https://era.zksync.network", apiKeys)
            )
        }

    }

}
