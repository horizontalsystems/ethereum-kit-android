package io.horizontalsystems.ethereum.kit.android

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.ethereum.kit.EthereumKit
import io.horizontalsystems.ethereum.kit.models.Transaction
import io.horizontalsystems.ethereum.kit.network.NetworkType

class MainViewModel : ViewModel(), EthereumKit.Listener {

    enum class State {
        STARTED, STOPPED
    }

    val transactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Long>()
    val lastBlockHeight = MutableLiveData<Int>()
    val status = MutableLiveData<State>()

    private var started = false
        set(value) {
            field = value
            status.value = (if (value) State.STARTED else State.STOPPED)
        }

    private var ethereumKit: EthereumKit

    init {
        val words = listOf("subway", "plate", "brick", "pattern", "inform", "used", "oblige", "identify", "cherry", "drop", "flush", "balance")
        ethereumKit = EthereumKit(words, NetworkType.Ropsten)

        ethereumKit.listener = this

        transactions.value = ethereumKit.transactions
//        balance.value = ethereumKit.balance
//        lastBlockHeight.value = ethereumKit.lastBlockHeight

        started = false
    }

    fun start() {
//        if (started) return
//        started = true

        ethereumKit.start()
    }

    fun receiveAddress(): String {
        return ""//ethereumKit.receiveAddress()
    }

    fun send(address: String, amount: Int) {
        //ethereumKit.send(address, amount)
    }

    override fun transactionsUpdated(ethereumKit: EthereumKit, inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        transactions.value = ethereumKit.transactions
    }

    override fun balanceUpdated(ethereumKit: EthereumKit, balance: Long) {
//        this.balance.value = balance
    }

//    override fun lastBlockInfoUpdated(ethereumKit: EthereumKit, lastBlockInfo: BlockInfo) {
//        this.lastBlockHeight.value = lastBlockInfo.height
//    }

    override fun progressUpdated(ethereumKit: EthereumKit, progress: Double) {
//        TODO("not implemented")
    }
}
