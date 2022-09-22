package io.horizontalsystems.nftkit.core

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.ethereumkit.core.IMethodDecorator

class Eip721MethodDecorator(
    private val contractMethodFactories: ContractMethodFactories
) : IMethodDecorator {
    override fun contractMethod(input: ByteArray): ContractMethod? {
        return contractMethodFactories.createMethodFromInput(input)
    }
}