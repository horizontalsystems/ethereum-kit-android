package io.horizontalsystems.ethereumkit.api.core

import io.horizontalsystems.ethereumkit.api.jsonrpc.JsonRpc
import kotlinx.coroutines.CancellationException

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
    @Volatile
    private var preferredIndex = 0

    @Volatile
    private var preferredSince = 0L

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
        val index = preferredIndex
        if (index != 0 && clock() - preferredSince >= preferredTtlMs) {
            preferredIndex = 0
            return 0
        }
        return index
    }

    private fun order(): List<Int> {
        val start = currentPreferredIndex()
        return endpoints.indices.map { (start + it) % endpoints.size }
    }

    private fun onAnswer(index: Int) {
        if (index != preferredIndex) {
            preferredIndex = index
            preferredSince = clock()
        }
    }

    companion object {
        const val PREFERRED_TTL_MS = 5 * 60 * 1000L
    }
}
