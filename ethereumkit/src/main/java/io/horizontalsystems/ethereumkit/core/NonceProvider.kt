package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.reactivex.Single

class NonceProvider : INonceProvider {
    private val providers = mutableListOf<INonceProvider>()

    fun addProvider(provider: INonceProvider) {
        providers.add(provider)
    }

    override fun getNonce(defaultBlockParameter: DefaultBlockParameter): Single<Long> {
        val singles = providers.map {
            it.getNonce(defaultBlockParameter)
        }

        return Single
            .zip(singles) { objects: Array<Any> ->
                var maxNonce = -1L
                for (obj in objects) {
                    if (obj is Long) {
                        maxNonce = maxOf(maxNonce, obj)
                    }
                }
                maxNonce
            }
            .flatMap { nonce: Long ->
                if (nonce == -1L) {
                    Single.error(IllegalStateException("Could not fetch nonce. None of the providers returned a value."))
                } else {
                    Single.just(nonce)
                }
            }
    }
}