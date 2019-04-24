package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.core.toHexString
import io.reactivex.subjects.PublishSubject
import java.math.BigInteger

class TokenHolder : ITokenHolder {

    class Token(val contractAddress: ByteArray,
                val balancePosition: Int,
                var balance: TokenBalance) {
        var syncState: SyncState = SyncState.NotSynced

        val syncStateSubject = PublishSubject.create<SyncState>()
        val balanceSubject = PublishSubject.create<BigInteger>()
        val transactionsSubject = PublishSubject.create<List<TransactionInfo>>()
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

    override fun syncStateSubject(contractAddress: ByteArray): PublishSubject<SyncState> {
        return token(contractAddress).syncStateSubject
    }

    override fun balanceSubject(contractAddress: ByteArray): PublishSubject<BigInteger> {
        return token(contractAddress).balanceSubject
    }

    override fun transactionsSubject(contractAddress: ByteArray): PublishSubject<List<TransactionInfo>> {
        return token(contractAddress).transactionsSubject
    }

    override fun register(contractAddress: ByteArray, balancePosition: Int, balance: TokenBalance) {
        val token = Token(contractAddress, balancePosition, balance)

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
