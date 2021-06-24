package io.horizontalsystems.erc20kit.decorations

import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransferMethodDecoration(val to: Address, val value: BigInteger) : ContractMethodDecoration() {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(toAddress.hex, "eip20Transfer")

        if (fromAddress == userAddress) {
            tags.add("${toAddress.hex}_outgoing")
            tags.add("outgoing")
        }

        if (to == userAddress) {
            tags.add("${toAddress.hex}_incoming")
            tags.add("incoming")
        }

        return tags
    }
}
