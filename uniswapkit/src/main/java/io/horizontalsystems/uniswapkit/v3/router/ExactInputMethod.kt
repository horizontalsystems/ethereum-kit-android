package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class ExactInputMethod(
    val path: ByteArray,
    val recipient: Address,
    val amountIn: BigInteger,
    val amountOutMinimum: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = throw Exception("This class has its own implementation of encodedABI()")

    override fun encodedABI(): ByteArray {
        val function = org.web3j.abi.datatypes.Function(
            "exactInput",
            listOf(
                DynamicStruct(
                    DynamicBytes(path),
                    org.web3j.abi.datatypes.Address(recipient.hex),
                    Uint256(amountIn),
                    Uint256(amountOutMinimum)
                )
            ),
            listOf()
        )

        return FunctionEncoder.encode(function).hexStringToByteArray()
    }

    companion object {
        private const val methodSignature = "exactInput((bytes,address,uint256,uint256))"
    }

    class Factory : ContractMethodFactory {
        override val methodId = ContractMethodHelper.getMethodId(methodSignature)

        override fun createMethod(inputArguments: ByteArray): ContractMethod {
            val argumentTypes = listOf(
                ContractMethodHelper.DynamicStruct(
                    listOf(
                        ByteArray::class,
                        Address::class,
                        BigInteger::class,
                        BigInteger::class
                    )
                ),
            )
            val dynamicStruct = ContractMethodHelper.decodeABI(inputArguments, argumentTypes)

            val parsedArguments = dynamicStruct.first() as List<Any>

            return ExactInputMethod(
                path = parsedArguments[0] as ByteArray,
                recipient = parsedArguments[1] as Address,
                amountIn = parsedArguments[2] as BigInteger,
                amountOutMinimum = parsedArguments[3] as BigInteger,
            )
        }
    }
}

val ExactInputMethod.tokenIn: Address
    get() = Address(path.copyOfRange(0, 20))

val ExactInputMethod.tokenOut: Address
    get() = Address(path.copyOfRange(path.size - 20, path.size))

val ExactOutputMethod.tokenIn: Address
    get() = Address(path.copyOfRange(path.size - 20, path.size))

val ExactOutputMethod.tokenOut: Address
    get() = Address(path.copyOfRange(0, 20))

