package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

open class ContractEventDecoration(val contractAddress: Address): TransactionDecoration()
