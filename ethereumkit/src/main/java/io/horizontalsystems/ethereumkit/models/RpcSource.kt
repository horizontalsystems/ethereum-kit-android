package io.horizontalsystems.ethereumkit.models

import java.net.URL

sealed class RpcSource() {
    class Http(val urls: List<URL>, val auth: String?) : RpcSource()
    class WebSocket(val url: URL, val auth: String?) : RpcSource()

    companion object {
        private fun infuraHttp(subdomain: String, projectId: String, projectSecret: String? = null): RpcSource {
            return RpcSource.Http(listOf(URL("https://$subdomain.infura.io/v3/$projectId")), projectSecret)
        }

        private fun infuraWebSocket(subdomain: String, projectId: String, projectSecret: String? = null): RpcSource {
            return RpcSource.WebSocket(URL("https://$subdomain.infura.io/ws/v3/$projectId"), projectSecret)
        }

        fun ethereumInfuraHttp(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraHttp("mainnet", projectId, projectSecret)
        }

        fun ropstenInfuraHttp(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraHttp("ropsten", projectId, projectSecret)
        }

        fun kovanInfuraHttp(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraHttp("kovan", projectId, projectSecret)
        }

        fun rinkebyInfuraHttp(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraHttp("rinkeby", projectId, projectSecret)
        }

        fun goerliInfuraHttp(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraHttp("goerli", projectId, projectSecret)
        }

        fun ethereumInfuraWebSocket(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraWebSocket("mainnet", projectId, projectSecret)
        }

        fun ropstenInfuraWebSocket(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraWebSocket("ropsten", projectId, projectSecret)
        }

        fun kovanInfuraWebSocket(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraWebSocket("kovan", projectId, projectSecret)
        }

        fun rinkebyInfuraWebSocket(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraWebSocket("rinkeby", projectId, projectSecret)
        }

        fun goerliInfuraWebSocket(projectId: String, projectSecret: String? = null): RpcSource {
            return infuraWebSocket("goerli", projectId, projectSecret)
        }

        fun binanceSmartChainHttp(): RpcSource {
            return RpcSource.Http(
                    listOf(
                            URL("https://bsc-dataseed.binance.org/"),
                            URL("https://bsc-dataseed1.defibit.io/"),
                            URL("https://bsc-dataseed1.ninicoin.io/"),
                            URL("https://bsc-dataseed2.defibit.io/"),
                            URL("https://bsc-dataseed3.defibit.io/"),
                            URL("https://bsc-dataseed4.defibit.io/"),
                            URL("https://bsc-dataseed2.ninicoin.io/"),
                            URL("https://bsc-dataseed3.ninicoin.io/"),
                            URL("https://bsc-dataseed4.ninicoin.io/"),
                            URL("https://bsc-dataseed1.binance.org/"),
                            URL("https://bsc-dataseed2.binance.org/"),
                            URL("https://bsc-dataseed3.binance.org/"),
                            URL("https://bsc-dataseed4.binance.org/")
                    ),
                    null
            )
        }

        fun binanceSmartChainWebSocket(): RpcSource {
            return RpcSource.WebSocket(URL("https://bsc-ws-node.nariox.org:443"), null)
        }

        fun polygonRpcHttp(): RpcSource {
            return RpcSource.Http(listOf(URL("https://polygon-rpc.com")), null)
        }

        fun optimismRpcHttp(): RpcSource {
            return RpcSource.Http(listOf(URL("https://mainnet.optimism.io")), null)
        }

        fun arbitrumOneRpcHttp(): RpcSource {
            return RpcSource.Http(listOf(URL("https://arb1.arbitrum.io/rpc")), null)
        }

    }
}
