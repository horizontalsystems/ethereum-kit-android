package io.horizontalsystems.erc20kit.events

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransferEventDecoration(
        val contractAddress: Address, val from: Address, val to: Address, val value: BigInteger
) : EventDecoration(contractAddress) {
    override val tags: List<String> = listOf(contractAddress.hex, "eip20Transfer")

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
