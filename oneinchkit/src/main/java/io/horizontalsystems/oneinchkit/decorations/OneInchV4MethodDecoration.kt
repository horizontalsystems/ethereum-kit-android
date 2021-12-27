package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag

class OneInchV4MethodDecoration : OneInchMethodDecoration() {
    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        return mutableListOf(toAddress.hex, TransactionTag.SWAP)
    }
}
