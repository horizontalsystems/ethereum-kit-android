package io.horizontalsystems.nftkit.events

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.nftkit.models.TokenInfo
import java.math.BigInteger

class Eip721TransferEventInstance(
    contractAddress: Address,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val tokenInfo: TokenInfo? = null
) : ContractEventInstance(contractAddress) {

    override fun tags(userAddress: Address) = buildList {
        add(contractAddress.hex)

        if (from == userAddress) {
            add("${contractAddress.hex}_outgoing")
            add("outgoing")
        }

        if (to == userAddress) {
            add("${contractAddress.hex}_incoming")
            add("incoming")
        }
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
