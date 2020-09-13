package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils

interface Erc20MethodFactory {
    val methodSignature: String
    val methodId: ByteArray
        get() = CryptoUtils.sha3(methodSignature.toByteArray()).copyOfRange(0, 4)

    fun createMethod(inputArguments: ByteArray): Erc20Method
}
