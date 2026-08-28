package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NonceProvider : INonceProvider {
    private val providers = mutableListOf<INonceProvider>()

    fun addProvider(provider: INonceProvider) {
        providers.add(provider)
    }

    override suspend fun getNonce(defaultBlockParameter: DefaultBlockParameter): Long {
        val nonces = coroutineScope {
            providers.map { provider ->
                async { provider.getNonce(defaultBlockParameter) }
            }.awaitAll()
        }

        val maxNonce = nonces.fold(-1L) { acc, nonce -> maxOf(acc, nonce) }

        if (maxNonce == -1L) {
            throw IllegalStateException("Could not fetch nonce. None of the providers returned a value.")
        }
        return maxNonce
    }
}
