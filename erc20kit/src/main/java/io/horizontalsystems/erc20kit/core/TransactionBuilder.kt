package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.network.ERC20
import java.math.BigInteger

class TransactionBuilder : ITransactionBuilder {

    override fun transferTransactionInput(toAddress: Address, value: BigInteger): ByteArray {
        return ERC20.encodeFunctionTransfer(toAddress, value)
    }
}
