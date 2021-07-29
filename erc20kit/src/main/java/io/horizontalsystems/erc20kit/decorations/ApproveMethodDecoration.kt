package io.horizontalsystems.erc20kit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class ApproveMethodDecoration(val spender: Address, val value: BigInteger) : ContractMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        return listOf(toAddress.hex, TransactionTag.EIP20_APPROVE)
    }
}
