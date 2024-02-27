package io.horizontalsystems.nftkit.events

import io.horizontalsystems.ethereumkit.contracts.ContractEvent
import io.horizontalsystems.ethereumkit.contracts.ContractEventInstance
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import io.horizontalsystems.nftkit.models.TokenInfo
import java.math.BigInteger

class Eip1155TransferEventInstance(
    contractAddress: Address,
    val from: Address,
    val to: Address,
    val tokenId: BigInteger,
    val value: BigInteger,
    val tokenInfo: TokenInfo? = null
): ContractEventInstance(contractAddress) {

    override fun tags(userAddress: Address): List<String> {
        return buildList {
            add(contractAddress.hex)

            if (from == userAddress) {
                add(TransactionTag.tokenOutgoing(contractAddress.hex))
                add(TransactionTag.OUTGOING)
                add(TransactionTag.toAddress(to.hex))
            }

            if (to == userAddress) {
                add(TransactionTag.tokenIncoming(contractAddress.hex))
                add(TransactionTag.INCOMING)
                add(TransactionTag.fromAddress(from.hex))
            }
        }
    }

    companion object {
        val transferSingleSignature = ContractEvent(
            "TransferSingle",
            listOf(
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Uint256,
                ContractEvent.Argument.Uint256,
            )
        ).signature
        val transferBatchSignature = ContractEvent(
            "TransferBatch",
            listOf(
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Address,
                ContractEvent.Argument.Uint256Array,
                ContractEvent.Argument.Uint256Array,
            )
        ).signature

    }


}
