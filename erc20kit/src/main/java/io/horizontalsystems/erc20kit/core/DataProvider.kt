package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.BalanceOfMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import java.math.BigInteger

class DataProvider(
        private val ethereumKit: EthereumKit
) : IDataProvider {

    override suspend fun getBalance(contractAddress: Address, address: Address): BigInteger {
        return ethereumKit.call(contractAddress, BalanceOfMethod(address).encodedABI())
                .sliceArray(IntRange(0, 31)).toBigInteger()
    }

}
