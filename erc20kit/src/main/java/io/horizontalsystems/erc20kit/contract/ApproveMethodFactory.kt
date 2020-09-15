package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger

object ApproveMethodFactory : Erc20MethodFactory {
    override fun createMethod(inputArguments: ByteArray): ApproveMethod {
        val address = Address(inputArguments.copyOfRange(12, 32))
        val value = inputArguments.copyOfRange(32, 64).toBigInteger()

        return ApproveMethod(address, value)
    }

    override fun canCreateMethod(methodId: ByteArray): Boolean {
        return ApproveMethod.methodId.contentEquals(methodId)
    }
}
