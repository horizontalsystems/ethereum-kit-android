package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

abstract class ContractEventDecoration(val contractAddress: Address) {
    abstract fun tags(userAddress: Address): List<String>
}
