package io.horizontalsystems.ethereumkit.contracts

interface ContractMethodFactory {

    val methodId: ByteArray
    fun createMethod(inputArguments: ByteArray): ContractMethod

}
