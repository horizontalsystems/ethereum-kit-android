package io.horizontalsystems.ethereumkit.api.models

import io.horizontalsystems.ethereumkit.core.ERC20
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

class State {
    var balance: BigInteger? = null
    var lastBlockHeight: Long? = null

    val erc20List = ConcurrentHashMap<String, ERC20>()

    val erc20Listeners: List<EthereumKit.Listener>
        get() {
            val listeners = mutableListOf<EthereumKit.Listener>()
            erc20List.values.forEach {
                listeners.add(it.listener)
            }
            return listeners
        }

    fun clear() {
        balance = null
        lastBlockHeight = null
        erc20List.clear()
    }

    fun add(contractAddress: ByteArray, listener: EthereumKit.Listener) {
        erc20List[contractAddress.toHexString()] = ERC20(contractAddress, listener)
    }

    fun hasContract(contractAddress: ByteArray): Boolean {
        return erc20List.containsKey(contractAddress.toHexString())
    }

    fun remove(contractAddress: ByteArray) {
        erc20List.remove(contractAddress.toHexString())
    }

    fun balance(contractAddress: ByteArray): BigInteger? {
        return erc20List[contractAddress.toHexString()]?.balance
    }

    fun listener(contractAddress: ByteArray): EthereumKit.Listener? {
        return erc20List[contractAddress.toHexString()]?.listener
    }

    fun setBalance(balance: BigInteger?, contractAddress: ByteArray) {
        erc20List[contractAddress.toHexString()]?.balance = balance
    }

}
