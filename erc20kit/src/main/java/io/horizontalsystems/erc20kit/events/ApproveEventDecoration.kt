package io.horizontalsystems.erc20kit.events

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.decorations.EventDecoration
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class ApproveEventDecoration(
        val contractAddress: Address, val owner: Address, val spender: Address, val value: BigInteger
) : EventDecoration(contractAddress) {
    override val tags: List<String> = listOf(contractAddress.hex, "eip20Approve")

    companion object {
        val signature = ContractEvent(
                "Approval",
                listOf(
                        ContractEvent.Argument.Address,
                        ContractEvent.Argument.Address,
                        ContractEvent.Argument.Uint256
                )
        ).signature
    }
}
