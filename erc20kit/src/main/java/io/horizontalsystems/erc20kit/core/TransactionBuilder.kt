package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransactionBuilder : ITransactionBuilder {

    override fun transferTransactionInput(to: Address, value: BigInteger): ByteArray {
        return TransferMethod(to, value).encodedABI()
    }
}
