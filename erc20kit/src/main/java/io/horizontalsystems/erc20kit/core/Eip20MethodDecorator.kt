package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.ethereumkit.core.IMethodDecorator

class Eip20MethodDecorator(
    private val contractMethodFactories: ContractMethodFactories
) : IMethodDecorator {

    override fun contractMethod(input: ByteArray): ContractMethod? =
        contractMethodFactories.createMethodFromInput(input)

}
