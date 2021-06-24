package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

open class TransactionDecoration {
    open fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        throw Exception("Method must be implemented by subclass")
    }
}
