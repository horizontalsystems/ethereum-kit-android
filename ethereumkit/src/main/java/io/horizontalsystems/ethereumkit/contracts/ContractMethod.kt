package io.horizontalsystems.ethereumkit.contracts

abstract class ContractMethod {
    val methodId: ByteArray by lazy { ContractMethodHelper.getMethodId(methodSignature) }

    protected abstract val methodSignature: String

    fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, getArguments())
    }

    protected abstract fun getArguments(): List<Any>
}
