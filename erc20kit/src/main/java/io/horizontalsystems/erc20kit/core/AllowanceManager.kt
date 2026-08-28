package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.AllowanceMethod
import io.horizontalsystems.erc20kit.contract.ApproveMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toRawHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import java.math.BigInteger

class AllowanceManager(
        private val ethereumKit: EthereumKit,
        private val contractAddress: Address,
        private val address: Address
) {

    suspend fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter): BigInteger {
        val result = ethereumKit
                .call(contractAddress, AllowanceMethod(address, spenderAddress).encodedABI(), defaultBlockParameter)
        return BigInteger(result.sliceArray(0..31).toRawHexString(), 16)
    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return TransactionData(contractAddress, BigInteger.ZERO, ApproveMethod(spenderAddress, amount).encodedABI())
    }

}
