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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class Eip20Provider(private val provider: IRpcApiProvider) {

    class TokenNotFoundException : Throwable()

    suspend fun getTokenInfo(contractAddress: Address): TokenInfo = coroutineScope {
        val name = async { getTokenName(contractAddress) }
        val symbol = async { getTokenSymbol(contractAddress) }
        val decimals = async { getDecimals(contractAddress) }

        TokenInfo(name.await(), symbol.await(), decimals.await())
    }

    private suspend fun getDecimals(contractAddress: Address): Int {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            DecimalsMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        val result = provider.execute(callRpc)
        if (result.isEmpty()) throw TokenNotFoundException()

        return result.sliceArray(IntRange(0, 31)).toBigInteger().toInt()
    }

    private suspend fun getTokenSymbol(contractAddress: Address): String {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            SymbolMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        return decodeString(provider.execute(callRpc))
    }

    private suspend fun getTokenName(contractAddress: Address): String {
        val callRpc = RpcBlockchain.callRpc(
            contractAddress,
            NameMethod().encodedABI(),
            DefaultBlockParameter.Latest
        )

        return decodeString(provider.execute(callRpc))
    }

    private fun decodeString(result: ByteArray): String {
        if (result.isEmpty()) throw TokenNotFoundException()

        val argumentTypes = listOf(ByteArray::class)

        val parsedArguments = ContractMethodHelper.decodeABI(result, argumentTypes)
        val stringBytes = parsedArguments[0] as? ByteArray ?: throw TokenNotFoundException()

        return String(stringBytes)
    }

    companion object {

        fun instance(rpcSource: RpcSource.Http): Eip20Provider {
            return Eip20Provider(RpcApiProviderFactory.nodeApiProvider(rpcSource))
        }

    }

}