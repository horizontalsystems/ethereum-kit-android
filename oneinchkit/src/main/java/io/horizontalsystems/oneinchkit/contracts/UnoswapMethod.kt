package io.horizontalsystems.oneinchkit.contracts

import io.horizontalsystems.ethereumkit.contracts.Bytes32Array
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class UnoswapMethod(
        val srcToken: Address,
        val amount: BigInteger,
        val minReturn: BigInteger,
        val params: Bytes32Array
) : ContractMethod() {

    override val methodSignature = Companion.methodSignature

    override fun getArguments() = listOf(srcToken, amount, minReturn, params)

    companion object {
        val methodSignature = "unoswap(address,uint256,uint256,bytes32[])"
    }

}
