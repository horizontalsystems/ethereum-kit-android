package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import kotlinx.coroutines.CancellationException
import java.util.concurrent.atomic.AtomicReference

/**
 * Runs a request against a list of endpoints, falling over to the next one when an endpoint
 * fails with anything but a JSON-RPC error (a JSON-RPC error is the chain's answer, not a dead
 * endpoint, and every other node would give the same one).
 *
 * The endpoint that last answered stays preferred, so a dead first endpoint does not cost a
 * timeout on every request. After [preferredTtlMs] the first endpoint is tried first again: it
 * is the one the user selected, and it is usually back by then.
 */
internal class UriFailover<E>(
    private val endpoints: List<E>,
    private val preferredTtlMs: Long = PREFERRED_TTL_MS,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private class Preference(val index: Int, val since: Long)

    // The index and its time are one value: kept apart, a request could read the new index with
    // the old time, take the preference for expired and go back to the first endpoint.
    private val preference = AtomicReference(Preference(0, 0L))

    val preferred: E?
        get() = endpoints.getOrNull(currentPreferredIndex())

    suspend fun <T> execute(attempt: suspend (E) -> T): T {
        var error: Throwable = NodeApiProvider.ApiProviderError.ApiUrlNotFound

        for (index in order()) {
            try {
                val result = attempt(endpoints[index])
                onAnswer(index)
                return result
            } catch (throwable: CancellationException) {
                throw throwable
            } catch (throwable: Throwable) {
                error = throwable
                if (throwable is JsonRpc.ResponseError.RpcError) {
                    // The endpoint is alive: it answered, only with an error.
                    onAnswer(index)
                    break
                }
            }
        }
        throw error
    }

    private fun currentPreferredIndex(): Int {
        val current = preference.get()
        if (current.index != 0 && clock() - current.since >= preferredTtlMs) {
            // Not when another request has set a newer preference meanwhile
            preference.compareAndSet(current, Preference(0, 0L))
            return preference.get().index
        }
        return current.index
    }

    private fun order(): List<Int> {
        val start = currentPreferredIndex()
        return endpoints.indices.map { (start + it) % endpoints.size }
    }

    private fun onAnswer(index: Int) {
        if (index != preference.get().index) {
            preference.set(Preference(index, clock()))
        }
    }

    companion object {
        const val PREFERRED_TTL_MS = 5 * 60 * 1000L
    }
}
