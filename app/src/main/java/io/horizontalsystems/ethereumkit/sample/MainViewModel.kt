package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.KitState
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction

class MainViewModel : ViewModel(), EthereumKit.Listener {

    val transactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Double>()
    val lastBlockHeight = MutableLiveData<Int>()
    val fee = MutableLiveData<Double>()
    val kitState = MutableLiveData<KitState>()
    val sendStatus = SingleLiveEvent<Throwable?>()

    private var ethereumKit: EthereumKit

    init {
        val words = listOf("subway", "plate", "brick", "pattern", "inform", "used", "oblige", "identify", "cherry", "drop", "flush", "balance")
        ethereumKit = EthereumKit(words, NetworkType.Kovan)

        ethereumKit.listener = this

        transactions.value = ethereumKit.transactions
        balance.value = ethereumKit.balance
        fee.value = ethereumKit.fee()
        ethereumKit.start()
    }

    fun refresh() {
        ethereumKit.refresh()
        fee.postValue(ethereumKit.fee())
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress()
    }

    fun send(address: String, amount: Double) {
        ethereumKit.send(address, amount) { error ->
            sendStatus.value = error
        }
    }

    override fun transactionsUpdated(inserted: List<Transaction>, updated: List<Transaction>, deleted: List<Int>) {
        transactions.postValue(ethereumKit.transactions)
    }

    override fun balanceUpdated(balance: Double) {
        this.balance.postValue(balance)
    }

    override fun lastBlockHeightUpdated(height: Int) {
        this.lastBlockHeight.postValue(height)
    }

    override fun onKitStateUpdate(state: KitState) {
        this.kitState.postValue(state)
    }
}
