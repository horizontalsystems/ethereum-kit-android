package io.horizontalsystems.erc20kit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class TransferMethodDecoration(val to: Address, val value: BigInteger) : ContractMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(toAddress.hex, TransactionTag.EIP20_TRANSFER)

        if (fromAddress == userAddress) {
            tags.add(TransactionTag.eip20Outgoing(toAddress.hex))
            tags.add(TransactionTag.OUTGOING)
        }

        if (to == userAddress) {
            tags.add(TransactionTag.eip20Incoming(toAddress.hex))
            tags.add(TransactionTag.INCOMING)
        }

        return tags
    }
}
