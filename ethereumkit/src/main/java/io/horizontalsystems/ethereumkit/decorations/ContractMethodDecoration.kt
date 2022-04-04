package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

abstract class ContractMethodDecoration {
    abstract fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String>
}
