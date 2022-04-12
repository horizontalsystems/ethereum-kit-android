package io.horizontalsystems.oneinchkit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.IMethodDecorator
import io.horizontalsystems.oneinchkit.contracts.OneInchContractMethodFactories

class OneInchMethodDecorator(private val contractMethodFactories: OneInchContractMethodFactories) : IMethodDecorator {

    override fun contractMethod(input: ByteArray): ContractMethod? =
        contractMethodFactories.createMethodFromInput(input)

}
