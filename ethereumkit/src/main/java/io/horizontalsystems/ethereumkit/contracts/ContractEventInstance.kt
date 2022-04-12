package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.models.Address

open class ContractEventInstance(val contractAddress: Address) {

    open fun tags(userAddress: Address): List<String> = listOf()

}
