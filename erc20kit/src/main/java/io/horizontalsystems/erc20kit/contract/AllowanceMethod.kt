package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address

class AllowanceMethod(val owner: Address, val spender: Address) : Erc20Method {
    override fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, listOf(owner, spender))
    }

    companion object {
        val methodId = ContractMethodHelper.getMethodId("allowance(address,address)")
    }
}
