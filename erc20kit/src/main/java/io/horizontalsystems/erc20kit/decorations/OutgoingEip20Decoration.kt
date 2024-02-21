package io.horizontalsystems.erc20kit.decorations

import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionTag
import java.math.BigInteger

class OutgoingEip20Decoration(
    val contractAddress: Address,
    val to: Address,
    val value: BigInteger,
    val sentToSelf: Boolean,
    val tokenInfo: TokenInfo?
) : TransactionDecoration {

    override fun tags() = listOf(
        contractAddress.hex,
        TransactionTag.EIP20_TRANSFER,
        TransactionTag.tokenOutgoing(contractAddress.hex),
        TransactionTag.OUTGOING,
        TransactionTag.toAddress(to.hex)
    )

}
