package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransferMethod(val to: Address, val value: BigInteger) : Erc20Method() {

    override val methodSignature = "transfer(address,uint256)"
    override fun getArguments() = listOf(to, value)

}
