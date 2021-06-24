package io.horizontalsystems.erc20kit.decorations

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.decorations.ContractEventDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransferEventDecoration(
        contractAddress: Address, val from: Address, val to: Address, val value: BigInteger
) : ContractEventDecoration(contractAddress) {

    override fun tags(fromAddress: Address, toAddress: Address, userAddress: Address): List<String> {
        val tags = mutableListOf(contractAddress.hex, "eip20Transfer")

        if (from == userAddress) {
            tags.add("${contractAddress.hex}_outgoing")
            tags.add("outgoing")
        }

        if (to == userAddress) {
            tags.add("${contractAddress.hex}_incoming")
            tags.add("incoming")
        }

        return tags
    }

    companion object {
        val signature = ContractEvent(
                "Transfer",
                listOf(
                        ContractEvent.Argument.Address,
                        ContractEvent.Argument.Address,
                        ContractEvent.Argument.Uint256
                )
        ).signature
    }
}
