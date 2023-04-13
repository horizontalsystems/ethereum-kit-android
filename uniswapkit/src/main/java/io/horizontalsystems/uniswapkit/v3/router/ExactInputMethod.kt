package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.uniswapkit.v3.SwapPath
import java.math.BigInteger

class ExactInputMethod(
    val path: SwapPath,
    val recipient: Address,
    val deadline: BigInteger,
    val amountIn: BigInteger,
    val amountOutMinimum: BigInteger,
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(
        path.abiEncodePacked(),
        recipient,
        deadline,
        amountIn,
        amountOutMinimum,
    )

    companion object {
        const val methodSignature = "exactInput((bytes,address,uint256,uint256,uint256))"
    }
}
