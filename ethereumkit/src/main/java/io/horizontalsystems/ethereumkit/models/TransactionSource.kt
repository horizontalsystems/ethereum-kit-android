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

        fun etherscanApi(apiKeys: List<String>): TransactionSource {
            return etherscan("api", null, apiKeys)
        }

    }

}
