package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Address

class AllowanceMethod(val owner: Address, val spender: Address) : Erc20Method() {

    override val methodSignature = "allowance(address,address)"
    override fun getArguments() = listOf(owner, spender)

}
