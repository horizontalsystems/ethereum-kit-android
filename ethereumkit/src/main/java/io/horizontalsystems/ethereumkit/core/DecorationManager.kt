package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData

class DecorationManager(
        private val address: Address
) {
    private val decorators = mutableListOf<IDecorator>()

    fun addDecorator(decorator: IDecorator) {
        decorators.add(decorator)
    }

    fun decorate(transactionData: TransactionData): TransactionDecoration? {
        if (transactionData.input.isEmpty())
            return TransactionDecoration.Transfer(address, transactionData.to, transactionData.value)

        for (decorator in decorators) {
            decorator.decorate(transactionData)?.let {
                return it
            }
        }
        return null
    }

}
