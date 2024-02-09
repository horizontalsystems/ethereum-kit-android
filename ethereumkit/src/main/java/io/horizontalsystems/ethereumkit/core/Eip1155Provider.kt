package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.api.core.IRpcApiProvider
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchain
import io.horizontalsystems.ethereumkit.contracts.ContractMethod
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single
import java.math.BigInteger

class Eip1155Provider(
    private val provider: IRpcApiProvider
) {

    class BalanceOfMethod(val owner: Address, val tokenId: BigInteger) : ContractMethod() {
        override val methodSignature = "balanceOf(address,uint256)"
        override fun getArguments() = listOf(owner, tokenId)
    }

    fun getTokenBalance(contractAddress: Address, tokenId: BigInteger, address: Address): Single<BigInteger> {
        val callRpc = RpcBlockchain.callRpc(contractAddress, BalanceOfMethod(address, tokenId).encodedABI(), DefaultBlockParameter.Latest)

        return provider
            .single(callRpc)
            .map { it.sliceArray(IntRange(0, 31)).toBigInteger() }
    }

    companion object {

        fun instance(rpcSource: RpcSource.Http): Eip1155Provider {
            val apiProvider = RpcApiProviderFactory.nodeApiProvider(rpcSource)

            return Eip1155Provider(apiProvider)
        }

    }

}
