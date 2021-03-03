package io.horizontalsystems.ethereumkit.contracts

import io.horizontalsystems.ethereumkit.spv.core.toInt

open class ContractMethodFactories {

    private val methodFactories = mutableMapOf<Int, ContractMethodFactory>()

    fun registerMethodFactories(factories: List<ContractMethodFactory>) {
        factories.forEach { factory ->
            methodFactories[factory.methodId.toInt()] = factory
        }
    }

    fun createMethodFromInput(input: ByteArray): ContractMethod? {
        val methodId = input.copyOfRange(0, 4)

        val erc20MethodFactory = methodFactories[methodId.toInt()]

        return erc20MethodFactory?.createMethod(input.copyOfRange(4, input.size))
    }

}
