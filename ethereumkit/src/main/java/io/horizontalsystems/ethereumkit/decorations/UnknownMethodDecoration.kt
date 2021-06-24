package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

class UnknownMethodDecoration(val method: ByteArray, val inputArguments: ByteArray) : ContractMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        return emptyList()
    }
}
