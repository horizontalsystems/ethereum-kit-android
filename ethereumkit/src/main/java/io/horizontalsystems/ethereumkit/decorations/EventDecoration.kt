package io.horizontalsystems.ethereumkit.decorations

import io.horizontalsystems.ethereumkit.models.Address

open class EventDecoration(contractAddress: Address) {
    open val tags: List<String> = listOf()
}
