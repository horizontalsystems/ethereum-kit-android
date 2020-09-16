package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.models.Address

class BalanceOfMethod(val owner: Address) : Erc20Method() {

    override val methodSignature = "balanceOf(address)"
    override fun getArguments() = listOf(owner)

}
