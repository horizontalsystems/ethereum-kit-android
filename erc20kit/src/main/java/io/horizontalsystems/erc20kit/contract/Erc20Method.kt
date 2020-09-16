package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper

abstract class Erc20Method {
    val methodId: ByteArray by lazy { ContractMethodHelper.getMethodId(methodSignature) }

    protected abstract val methodSignature: String

    fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, getArguments())
    }

    protected abstract fun getArguments(): List<Any>
}
