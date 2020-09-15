package io.horizontalsystems.erc20kit.contract

import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransferMethod(val to: Address, val value: BigInteger) : Erc20Method {
    override fun encodedABI(): ByteArray {
        return ContractMethodHelper.encodedABI(methodId, listOf(to, value))
    }

    companion object {
        val methodId = ContractMethodHelper.getMethodId("transfer(address,uint256)")
    }
}
