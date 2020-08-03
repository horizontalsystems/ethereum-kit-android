package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.AddressArgument
import io.horizontalsystems.ethereumkit.contracts.ContractMethod.Argument.Uint256Argument
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

class TransactionBuilder : ITransactionBuilder {

    override fun transferTransactionInput(to: Address, value: BigInteger): ByteArray {
        return ContractMethod("transfer", listOf(AddressArgument(to), Uint256Argument(value))).encodedABI()
    }
}
