package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration

class FullTransaction(
    val transaction: Transaction,
    val decoration: TransactionDecoration,
    val extra: Map<String, Any>
)
