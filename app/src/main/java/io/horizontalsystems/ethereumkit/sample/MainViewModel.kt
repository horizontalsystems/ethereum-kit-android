package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.KitState
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel(), EthereumKit.Listener {

    val transactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Double>()
    val lastBlockHeight = MutableLiveData<Int>()
    val fee = MutableLiveData<Double>()
    val kitState = MutableLiveData<KitState>()
    val sendStatus = SingleLiveEvent<Throwable?>()
    private val disposables = CompositeDisposable()

    private var ethereumKit: EthereumKit

    init {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")
        ethereumKit = EthereumKit(words, NetworkType.Ropsten)
        ethereumKit.include("0x583cbBb8a8443B38aBcC0c956beCe47340ea1367", 18)

        ethereumKit.listener = this

        ethereumKit.transactions().subscribe { txList: List<Transaction> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }
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
        ethereumKit.transactions().subscribe { txList: List<Transaction> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }
    }

    override fun balanceUpdated(address: String, balance: Double) {
        this.balance.postValue(balance)
    }

    override fun lastBlockHeightUpdated(height: Int) {
        this.lastBlockHeight.postValue(height)
    }

    override fun onKitStateUpdate(address: String?, state: KitState) {
        address?.let { return }
        this.kitState.postValue(state)
    }
}
