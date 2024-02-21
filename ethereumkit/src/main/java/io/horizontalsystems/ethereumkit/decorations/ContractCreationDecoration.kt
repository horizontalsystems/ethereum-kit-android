package io.horizontalsystems.ethereumkit.decorations

class ContractCreationDecoration : TransactionDecoration {
    override fun tags() = listOf("contractCreation")
}
