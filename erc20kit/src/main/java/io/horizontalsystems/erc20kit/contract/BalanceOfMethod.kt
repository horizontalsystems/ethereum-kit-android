package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address

class BalanceOfMethod(val owner: Address) : Erc20Method {
    override fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, listOf(owner))
    }

    companion object {
        val methodId = ContractMethodHelper.getMethodId("balanceOf(address)")
    }
}
