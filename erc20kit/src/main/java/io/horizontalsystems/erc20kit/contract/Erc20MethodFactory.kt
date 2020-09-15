package io.horizontalsystems.erc20kit.contract

interface Erc20MethodFactory {
    fun createMethod(inputArguments: ByteArray): Erc20Method
    fun canCreateMethod(methodId: ByteArray): Boolean
}
