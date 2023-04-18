package io.horizontalsystems.ethereumkit.contracts

open class ContractMethod {
    val methodId: ByteArray by lazy { ContractMethodHelper.getMethodId(methodSignature) }

    protected open val methodSignature: String = ""

    open fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, getArguments())
    }

    protected open fun getArguments(): List<Any> = listOf()
}
