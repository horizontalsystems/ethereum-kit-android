package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.erc20kit.models.Transaction

interface Erc20Method {
    fun getErc20Transactions(ethTx: io.horizontalsystems.ethereumkit.models.Transaction): List<Transaction> {
        return listOf()
    }

    fun encodedABI(): ByteArray
}
