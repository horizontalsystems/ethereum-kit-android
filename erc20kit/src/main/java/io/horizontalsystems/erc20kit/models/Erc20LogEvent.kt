package io.horizontalsystems.erc20kit.models

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

sealed class Erc20LogEvent {
    class Transfer(val from: Address, val to: Address, val value: BigInteger) : Erc20LogEvent() {
        companion object {
            val signature = ContractEvent("Transfer", listOf(ContractEvent.Argument.Address, ContractEvent.Argument.Address, ContractEvent.Argument.Uint256)).signature
        }
    }

    class Approve(val owner: Address, val spender: Address, val value: BigInteger) : Erc20LogEvent() {
        companion object {
            val signature = ContractEvent("Approval", listOf(ContractEvent.Argument.Address, ContractEvent.Argument.Address, ContractEvent.Argument.Uint256)).signature
        }
    }
}
