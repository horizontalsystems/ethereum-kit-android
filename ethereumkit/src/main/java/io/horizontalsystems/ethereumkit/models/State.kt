package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.core.ERC20
import java.util.concurrent.ConcurrentHashMap

class State {
    var balance: String? = null
    var lastBlockHeight: Int? = null

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

    fun add(contractAddress: String, listener: EthereumKit.Listener) {
        erc20List[contractAddress] = ERC20(contractAddress, listener)
    }

    fun hasContract(contractAddress: String): Boolean {
        return erc20List.containsKey(contractAddress)
    }

    fun remove(contractAddress: String) {
        erc20List.remove(contractAddress)
    }

    fun balance(contractAddress: String): String? {
        return erc20List[contractAddress]?.balance
    }

    fun listener(contractAddress: String): EthereumKit.Listener? {
        return erc20List[contractAddress]?.listener
    }

    fun setBalance(balance: String?, contractAddress: String) {
        erc20List[contractAddress]?.balance = balance
    }

}
