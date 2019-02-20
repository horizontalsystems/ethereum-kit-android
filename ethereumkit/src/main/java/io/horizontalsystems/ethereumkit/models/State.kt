package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.core.ERC20
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

class State {
    var balance: BigDecimal? = null
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

    fun add(contractAddress: String, decimal: Int, listener: EthereumKit.Listener) {
        erc20List[contractAddress] = ERC20(contractAddress, decimal, listener)
    }

    fun hasContract(contractAddress: String): Boolean {
        return erc20List.containsKey(contractAddress)
    }

    fun remove(contractAddress: String) {
        erc20List.remove(contractAddress)
    }

    fun balance(contractAddress: String): BigDecimal? {
        return erc20List[contractAddress]?.balance
    }

    fun listener(contractAddress: String): EthereumKit.Listener? {
        return erc20List[contractAddress]?.listener
    }

    fun setBalance(balance: BigDecimal, contractAddress: String) {
        erc20List[contractAddress]?.balance = balance
    }

}
