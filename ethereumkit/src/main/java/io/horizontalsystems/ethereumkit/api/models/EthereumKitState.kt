package io.horizontalsystems.ethereumkit.api.models

import java.math.BigInteger

class EthereumKitState {
    var balance: BigInteger? = null
    var lastBlockHeight: Long? = null

    fun clear() {
        balance = null
        lastBlockHeight = null
    }
}
