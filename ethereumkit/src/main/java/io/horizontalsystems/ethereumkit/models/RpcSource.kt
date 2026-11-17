package io.horizontalsystems.ethereumkit.models

import java.net.URI

sealed class RpcSource {
    /** One URL of an HTTP source, with the secret that belongs to it. */
    data class Endpoint(val uri: URI, val auth: String? = null)

    /**
     * The endpoints are tried in order. Each carries its own secret, so endpoints of different
     * providers can share one source and no provider receives another's secret.
     */
    data class Http(val endpoints: List<Endpoint>) : RpcSource() {
        /** A single provider: every one of [uris] gets the same [auth]. */
        constructor(uris: List<URI>, auth: String?) : this(uris.map { Endpoint(it, auth) })

        val uris: List<URI>
            get() = endpoints.map { it.uri }
    }
    data class WebSocket(val uri: URI, val auth: String?) : RpcSource()

    companion object {
        fun bscRpcHttp(): Http {
            return Http(listOf(URI("https://bscrpc.com")), null)
        }

        fun zkSyncRpcHttp(): Http {
            return Http(listOf(URI("https://mainnet.era.zksync.io")), null)
        }

        fun binanceSmartChainHttp(): Http {
            return Http(
                    listOf(
                            URI("https://bsc-dataseed.binance.org/"),
                            URI("https://bsc-dataseed1.defibit.io/"),
                            URI("https://bsc-dataseed1.ninicoin.io/"),
                            URI("https://bsc-dataseed2.defibit.io/"),
                            URI("https://bsc-dataseed3.defibit.io/"),
                            URI("https://bsc-dataseed4.defibit.io/"),
                            URI("https://bsc-dataseed2.ninicoin.io/"),
                            URI("https://bsc-dataseed3.ninicoin.io/"),
                            URI("https://bsc-dataseed4.ninicoin.io/"),
                            URI("https://bsc-dataseed1.binance.org/"),
                            URI("https://bsc-dataseed2.binance.org/"),
                            URI("https://bsc-dataseed3.binance.org/"),
                            URI("https://bsc-dataseed4.binance.org/")
                    ),
                    null
            )
        }

        fun polygonRpcHttp(): Http {
            return Http(
                listOf(
                    URI("https://polygon.drpc.org"),
                    URI("https://1rpc.io/matic")
                ),
                null)
        }

        fun optimismRpcHttp(): Http {
            return Http(listOf(URI("https://mainnet.optimism.io")), null)
        }

        fun arbitrumOneRpcHttp(): Http {
            return Http(listOf(URI("https://arb1.arbitrum.io/rpc")), null)
        }

        fun avaxNetworkHttp(): Http {
            return Http(listOf(URI("https://api.avax.network/ext/bc/C/rpc")), null)
        }

        fun gnosisRpcHttp(): Http {
            return Http(listOf(URI("https://rpc.gnosischain.com")), null)
        }

        fun fantomRpcHttp(): Http {
            return Http(listOf(URI("https://rpc.fantom.network")), null)
        }

        fun baseRpcHttp(): Http {
            return Http(listOf(URI("https://mainnet.base.org")), null)
        }

    }
}
