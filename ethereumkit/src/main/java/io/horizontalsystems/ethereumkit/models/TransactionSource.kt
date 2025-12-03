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

    }

}
