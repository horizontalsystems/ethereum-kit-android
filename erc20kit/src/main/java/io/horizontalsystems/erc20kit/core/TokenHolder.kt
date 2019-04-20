package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString

class TokenHolder : ITokenHolder {

    class Token(val contractAddress: ByteArray,
                val balancePosition: Int,
                var balance: TokenBalance) {
        var syncState: SyncState = SyncState.NotSynced
        var listener: IErc20TokenListener? = null
    }

    private val tokensMap = HashMap<String, Token>()

    private fun token(contractAddress: ByteArray): Token {
        return tokensMap[contractAddress.toHexString()] ?: throw Erc20Kit.NotRegisteredToken()
    }

    override val contractAddresses: List<ByteArray>
        get() = tokensMap.keys.toList().map { it.hexStringToByteArray() }

    override fun syncState(contractAddress: ByteArray): SyncState {
        return token(contractAddress).syncState
    }

    override fun balance(contractAddress: ByteArray): TokenBalance {
        return token(contractAddress).balance
    }

    override fun balancePosition(contractAddress: ByteArray): Int {
        return token(contractAddress).balancePosition
    }

    override fun listener(contractAddress: ByteArray): IErc20TokenListener? {
        return try {
            token(contractAddress).listener
        } catch (ex: Erc20Kit.NotRegisteredToken) {
            null
        }
    }

    override fun register(contractAddress: ByteArray, balancePosition: Int, balance: TokenBalance, listener: IErc20TokenListener) {
        val token = Token(contractAddress, balancePosition, balance)
        token.listener = listener

        tokensMap[contractAddress.toHexString()] = token
    }

    override fun unregister(contractAddress: ByteArray) {
        tokensMap.remove(contractAddress.toHexString())
    }

    override fun set(syncState: SyncState, contractAddress: ByteArray) {
        token(contractAddress).syncState = syncState
    }

    override fun set(balance: TokenBalance, contractAddress: ByteArray) {
        token(contractAddress).balance = balance
    }

    override fun clear() {
        tokensMap.clear()
    }
}