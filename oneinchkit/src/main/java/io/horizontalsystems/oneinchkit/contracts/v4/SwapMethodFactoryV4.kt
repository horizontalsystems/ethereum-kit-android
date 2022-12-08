package io.horizontalsystems.oneinchkit.contracts.v4

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactory
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class SwapMethodFactoryV4 : ContractMethodFactory {

    override val methodId = ContractMethodHelper.getMethodId(SwapMethodV4.methodSignature)

    override fun createMethod(inputArguments: ByteArray): ContractMethod {
        val argumentTypes = listOf(
                Address::class,
                ContractMethodHelper.StructParameter(listOf(Address::class, Address::class, Address::class, Address::class, BigInteger::class, BigInteger::class, BigInteger::class, ByteArray::class)),
                ByteArray::class
        )
        val parsedArguments = ContractMethodHelper.decodeABI(inputArguments, argumentTypes)

        val caller = parsedArguments[0] as Address
        val swapDescriptionArguments = parsedArguments[1] as List<*>

        val srcToken = swapDescriptionArguments[0] as Address
        val dstToken = swapDescriptionArguments[1] as Address
        val srcReceiver = swapDescriptionArguments[2] as Address
        val dstReceiver = swapDescriptionArguments[3] as Address
        val amount = swapDescriptionArguments[4] as BigInteger
        val minReturnAmount = swapDescriptionArguments[5] as BigInteger
        val flags = swapDescriptionArguments[6] as BigInteger
        val permit = swapDescriptionArguments[7] as ByteArray

        val swapDescription = SwapMethodV4.SwapDescription(srcToken, dstToken, srcReceiver, dstReceiver, amount, minReturnAmount, flags, permit)

        val data = parsedArguments[2] as ByteArray

        return SwapMethodV4(caller, swapDescription, data)
    }

}
