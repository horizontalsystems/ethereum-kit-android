package io.horizontalsystems.uniswapkit.v3.router

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class UnwrapWETH9Method(
    val amountMinimum: BigInteger,
    val recipient: Address
) : ContractMethod() {
    override val methodSignature = Companion.methodSignature
    override fun getArguments() = listOf(amountMinimum, recipient)

    companion object {
        const val methodSignature = "unwrapWETH9(uint256,address)"
    }
}
