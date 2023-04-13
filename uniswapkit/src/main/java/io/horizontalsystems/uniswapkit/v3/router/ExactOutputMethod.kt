package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger

class ExactOutputMethod(
    val path: SwapPath,
    val recipient: Address,
    val deadline: BigInteger,
    val amountOut: BigInteger,
    val amountInMaximum: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(
        path.abiEncodePacked(),
        recipient,
        deadline,
        amountOut,
        amountInMaximum,
    )

    companion object {
        const val methodSignature = "exactOutput((bytes,address,uint256,uint256,uint256))"
    }
}
