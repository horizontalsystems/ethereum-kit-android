package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.DecimalsMethod
import io.horizontalsystems.erc20kit.contract.NameMethod
import io.horizontalsystems.erc20kit.contract.SymbolMethod
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.ethereumkit.api.core.IRpcApiProvider
import io.horizontalsystems.ethereumkit.api.core.RpcBlockchain
import io.horizontalsystems.ethereumkit.contracts.ContractMethodHelper
import io.horizontalsystems.ethereumkit.core.RpcApiProviderFactory
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.RpcSource
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.reactivex.Single

class Eip20Provider(private val provider: IRpcApiProvider) {

    class TokenNotFoundException : Throwable()

    fun getTokenInfo(contractAddress: Address): Single<TokenInfo> {
        val nameSingle = getTokenName(contractAddress)
        val symbolSingle = getTokenSymbol(contractAddress)
        val decimalsSingle = getDecimals(contractAddress)

        return Single
            .zip(nameSingle, symbolSingle, decimalsSingle) { name, symbol, decimals ->
                TokenInfo(name, symbol, decimals)
            }
    }

    private fun getDecimals(contractAddress: Address): Single<Int> {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            DecimalsMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        return provider.single(callRpc)
            .map {
                if (it.isEmpty()) throw TokenNotFoundException()

                it.sliceArray(IntRange(0, 31)).toBigInteger().toInt()
            }
    }

    private fun getTokenSymbol(contractAddress: Address): Single<String> {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            SymbolMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        return provider.single(callRpc)
            .map {
                if (it.isEmpty()) throw TokenNotFoundException()

                val argumentTypes = listOf(ByteArray::class)

                val parsedArguments = ContractMethodHelper.decodeABI(it, argumentTypes)
                val stringBytes = parsedArguments[0] as? ByteArray ?: throw TokenNotFoundException()

                String(stringBytes)
            }
    }

    private fun getTokenName(contractAddress: Address): Single<String> {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            NameMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        return provider.single(callRpc)
            .map {
                if (it.isEmpty()) throw TokenNotFoundException()

                val argumentTypes = listOf(ByteArray::class)

                val parsedArguments = ContractMethodHelper.decodeABI(it, argumentTypes)
                val stringBytes = parsedArguments[0] as? ByteArray ?: throw TokenNotFoundException()

                String(stringBytes)
            }
    }

    companion object {

        fun instance(rpcSource: RpcSource.Http): Eip20Provider {
            return Eip20Provider(RpcApiProviderFactory.nodeApiProvider(rpcSource))
        }

    }

}