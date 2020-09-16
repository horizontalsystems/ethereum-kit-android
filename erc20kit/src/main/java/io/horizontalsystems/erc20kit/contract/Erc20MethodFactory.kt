package io.horizontalsystems.erc20kit.contract

interface Erc20MethodFactory {

    val methodId: ByteArray
    fun createMethod(inputArguments: ByteArray): Erc20Method

}
