package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.v3.SwapPath
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.DynamicBytes
import org.web3j.abi.datatypes.DynamicStruct
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger

class ExactOutputMethod(
    val path: SwapPath,
    val recipient: Address,
    val deadline: BigInteger,
    val amountOut: BigInteger,
    val amountInMaximum: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = throw Exception("This class has its own implementation of encodedABI()")

    override fun encodedABI(): ByteArray {
        val function = org.web3j.abi.datatypes.Function(
            "exactOutput",
            listOf(
                DynamicStruct(
                    DynamicBytes(path.abiEncodePacked()),
                    org.web3j.abi.datatypes.Address(recipient.hex),
                    Uint256(deadline),
                    Uint256(amountOut),
                    Uint256(amountInMaximum)
                )
            ),
            listOf()
        )

        return FunctionEncoder.encode(function).hexStringToByteArray()
    }

    companion object {
        const val methodSignature = "exactOutput((bytes,address,uint256,uint256,uint256))"
    }
}
