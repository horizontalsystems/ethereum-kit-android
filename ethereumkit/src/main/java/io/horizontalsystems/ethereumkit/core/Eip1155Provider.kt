package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
import java.math.BigInteger

class Eip1155Provider(
    private val evmKit: EthereumKit
) {

    class BalanceOfMethod(val owner: Address, val tokenId: BigInteger) : ContractMethod() {

        override val methodSignature = "balanceOf(address,uint256)"
        override fun getArguments() = listOf(owner, tokenId)

    }

    fun geTokenBalance(contractAddress: Address, tokenId: BigInteger, address: Address): Single<BigInteger> {
        return evmKit
            .call(contractAddress, BalanceOfMethod(address, tokenId).encodedABI())
            .map { it.sliceArray(IntRange(0, 31)).toBigInteger() }
    }

}
