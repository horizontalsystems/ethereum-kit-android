package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.BlockNumberJsonRpc
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.RpcSource
import kotlinx.coroutines.runBlocking
import okhttp3.Credentials
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

// The endpoints of one source may belong to different providers, so a provider must only ever
// receive its own secret.
class NodeApiProviderAuthTest {

    private class Server(private val alive: Boolean) {
        val authorizations = mutableListOf<String?>()

        private val server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    authorizations.add(request.getHeader("Authorization"))
                    return if (alive) {
                        MockResponse()
                            .setHeader("Content-Type", "application/json")
                            .setBody("""{"jsonrpc":"2.0","id":1,"result":"0x10"}""")
                    } else {
                        MockResponse().setResponseCode(503)
                    }
                }
            }
            start()
        }

        val uri: URI = server.url("/").toUri()

        fun stop() = server.shutdown()
    }

    private val down = Server(alive = false)
    private val up = Server(alive = true)

    @After
    fun tearDown() {
        down.stop()
        up.stop()
    }

    private fun blockNumber(source: RpcSource.Http): Long = runBlocking {
        NodeApiProvider(source.endpoints, EthereumKit.gson).execute(BlockNumberJsonRpc())
    }

    @Test
    fun eachEndpointReceivesItsOwnSecret() {
        val source = RpcSource.Http(
            listOf(
                RpcSource.Endpoint(down.uri, "first-secret"),
                RpcSource.Endpoint(up.uri, "second-secret"),
            )
        )

        assertEquals(16L, blockNumber(source))
        assertEquals(listOf<String?>(Credentials.basic("", "first-secret")), down.authorizations)
        assertEquals(listOf<String?>(Credentials.basic("", "second-secret")), up.authorizations)
    }

    @Test
    fun endpointWithoutASecretReceivesNone() {
        val source = RpcSource.Http(
            listOf(
                RpcSource.Endpoint(down.uri, "first-secret"),
                RpcSource.Endpoint(up.uri),
            )
        )

        assertEquals(16L, blockNumber(source))
        assertEquals(listOf<String?>(Credentials.basic("", "first-secret")), down.authorizations)
        assertEquals(listOf<String?>(null), up.authorizations)
    }

    @Test
    fun singleSecretGoesToEveryEndpoint() {
        val source = RpcSource.Http(listOf(down.uri, up.uri), "shared-secret")

        assertEquals(16L, blockNumber(source))
        assertEquals(listOf<String?>(Credentials.basic("", "shared-secret")), down.authorizations)
        assertEquals(listOf<String?>(Credentials.basic("", "shared-secret")), up.authorizations)
    }

    @Test
    fun noSecretSendsNoHeader() {
        val source = RpcSource.Http(listOf(up.uri), null)

        assertEquals(16L, blockNumber(source))
        assertEquals(listOf<String?>(null), up.authorizations)
    }
}
