package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger

class ApproveMethodFactory : Erc20MethodFactory {
    override val methodSignature = "approve(address,uint256)"

    override fun createMethod(inputArguments: ByteArray): ApproveMethod {
        val address = Address(inputArguments.copyOfRange(12, 32))
        val value = inputArguments.copyOfRange(32, 64).toBigInteger()

        return ApproveMethod(address, value)
    }
}
