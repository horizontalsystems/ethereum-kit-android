package io.horizontalsystems.ethereumkit.models

class TransactionSource(val name: String, val type: SourceType) {

    fun transactionUrl(hash: String) =
        when (type) {
            is SourceType.Etherscan -> "${type.txBaseUrl}/tx/$hash"
        }

    sealed class SourceType {
        class Etherscan(val apiBaseUrl: String, val txBaseUrl: String, val apiKey: String) : SourceType()
    }

    companion object {
        private fun etherscan(apiSubdomain: String, txSubdomain: String?, apiKey: String): TransactionSource {
            return TransactionSource(
                "etherscan.io",
                SourceType.Etherscan("https://$apiSubdomain.etherscan.io", "https://${txSubdomain?.let { "$it." } ?: ""}etherscan.io", apiKey)
            )
        }

        fun ethereumEtherscan(apiKey: String): TransactionSource {
            return etherscan("api", null, apiKey)
        }

        fun ropstenEtherscan(apiKey: String): TransactionSource {
            return etherscan("api-ropsten", "ropsten", apiKey)
        }

        fun kovanEtherscan(apiKey: String): TransactionSource {
            return etherscan("api-kovan", "kovan", apiKey)
        }

        fun rinkebyEtherscan(apiKey: String): TransactionSource {
            return etherscan("api-rinkeby", "rinkeby", apiKey)
        }

        fun goerliEtherscan(apiKey: String): TransactionSource {
            return etherscan("api-goerli", "goerli", apiKey)
        }

        fun bscscan(apiKey: String): TransactionSource {
            return TransactionSource(
                "bscscan.com",
                SourceType.Etherscan("https://api.bscscan.com", "https://bscscan.com", apiKey)
            )
        }

        fun polygonscan(apiKey: String): TransactionSource {
            return TransactionSource(
                "polygonscan.com",
                SourceType.Etherscan("https://api.polygonscan.com", "https://polygonscan.com", apiKey)
            )
        }

        fun optimisticEtherscan(apiKey: String): TransactionSource {
            return TransactionSource(
                "optimistic.etherscan.io",
                SourceType.Etherscan("https://api-optimistic.etherscan.io", "https://optimistic.etherscan.io", apiKey)
            )
        }

        fun arbiscan(apiKey: String): TransactionSource {
            return TransactionSource(
                "arbiscan.io",
                SourceType.Etherscan("https://api.arbiscan.io", "https://arbiscan.io", apiKey)
            )
        }

    }

}
