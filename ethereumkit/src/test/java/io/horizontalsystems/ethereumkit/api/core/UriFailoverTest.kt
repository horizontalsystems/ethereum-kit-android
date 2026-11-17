package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class UriFailoverTest {

    private var now = 0L
    private val calls = mutableListOf<String>()

    private fun failover(vararg endpoints: String) =
        UriFailover(endpoints.toList(), preferredTtlMs = 1_000, clock = { now })

    private fun answer(dead: Set<String>): suspend (String) -> String = { endpoint ->
        calls.add(endpoint)
        if (endpoint in dead) throw IOException("$endpoint is down")
        endpoint
    }

    @Test
    fun firstEndpointAnswers() = runTest {
        val failover = failover("a", "b", "c")

        assertEquals("a", failover.execute(answer(emptySet())))
        assertEquals(listOf("a"), calls)
    }

    @Test
    fun deadEndpointFallsOverToTheNext() = runTest {
        val failover = failover("a", "b", "c")

        assertEquals("b", failover.execute(answer(setOf("a"))))
        assertEquals(listOf("a", "b"), calls)
    }

    @Test
    fun answeringEndpointStaysPreferred() = runTest {
        val failover = failover("a", "b", "c")
        failover.execute(answer(setOf("a")))
        calls.clear()

        assertEquals("b", failover.execute(answer(setOf("a"))))
        assertEquals(listOf("b"), calls)
        assertEquals("b", failover.preferred)
    }

    @Test
    fun preferredEndpointFailingWrapsAround() = runTest {
        val failover = failover("a", "b", "c")
        failover.execute(answer(setOf("a", "b")))
        calls.clear()

        assertEquals("a", failover.execute(answer(setOf("c"))))
        assertEquals(listOf("c", "a"), calls)
    }

    @Test
    fun firstEndpointIsRetriedAfterTtl() = runTest {
        val failover = failover("a", "b")
        failover.execute(answer(setOf("a")))
        now = 1_000
        calls.clear()

        assertEquals("a", failover.execute(answer(emptySet())))
        assertEquals(listOf("a"), calls)
    }

    @Test
    fun rpcErrorDoesNotFallOver() = runTest {
        val failover = failover("a", "b")

        try {
            failover.execute<String> { endpoint ->
                calls.add(endpoint)
                throw JsonRpc.ResponseError.RpcError(RpcResponse.Error(-32000, "insufficient funds"))
            }
            fail("expected RpcError")
        } catch (e: JsonRpc.ResponseError.RpcError) {
            assertEquals(listOf("a"), calls)
        }
    }

    // An RPC error is an answer, so the endpoint that gave it is alive and the next request
    // must start there, not at the dead one before it.
    @Test
    fun endpointAnsweringWithRpcErrorBecomesPreferred() = runTest {
        val failover = failover("a", "b", "c")

        try {
            failover.execute<String> { endpoint ->
                calls.add(endpoint)
                if (endpoint == "a") throw IOException("a is down")
                throw JsonRpc.ResponseError.RpcError(RpcResponse.Error(-32000, "nonce too low"))
            }
            fail("expected RpcError")
        } catch (e: JsonRpc.ResponseError.RpcError) {
            assertEquals(listOf("a", "b"), calls)
        }
        calls.clear()

        assertEquals("b", failover.execute(answer(setOf("a"))))
        assertEquals(listOf("b"), calls)
    }

    @Test
    fun allDeadThrowsLastError() = runTest {
        val failover = failover("a", "b")

        try {
            failover.execute(answer(setOf("a", "b")))
            fail("expected IOException")
        } catch (e: IOException) {
            assertEquals("b is down", e.message)
        }
    }

    @Test
    fun alreadyKnownMessagesAreRecognised() {
        fun rpcError(message: String) = JsonRpc.ResponseError.RpcError(RpcResponse.Error(-32000, message))

        assertTrue(RpcBlockchain.isAlreadyKnown(rpcError("already known")))
        assertTrue(RpcBlockchain.isAlreadyKnown(rpcError("Known transaction: 0xabc")))
        assertTrue(RpcBlockchain.isAlreadyKnown(rpcError("Transaction already imported")))
        assertTrue(RpcBlockchain.isAlreadyKnown(rpcError("AlreadyKnown")))
        assertFalse(RpcBlockchain.isAlreadyKnown(rpcError("nonce too low")))
        assertFalse(RpcBlockchain.isAlreadyKnown(rpcError("insufficient funds for gas * price + value")))
    }
}
