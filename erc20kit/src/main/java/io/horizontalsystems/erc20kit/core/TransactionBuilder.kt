package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.network.ERC20
import java.math.BigInteger

class TransactionBuilder : ITransactionBuilder {

    override fun transferTransactionInput(toAddress: ByteArray, value: BigInteger): ByteArray {
        return ERC20.encodeFunctionTransfer(toAddress, value)
    }
}
