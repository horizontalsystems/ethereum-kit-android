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
    val balanceToken = MutableLiveData<Double>()
    val lastBlockHeight = MutableLiveData<Int>()
    val fee = MutableLiveData<Double>()
    val kitState = MutableLiveData<KitState>()
    val sendStatus = SingleLiveEvent<Throwable?>()
    private val disposables = CompositeDisposable()

    private var ethereumKit: EthereumKit
    private val contractAddress = "0xF559862f9265756619d5523bBC4bd8422898e97d"
    private val decimal = 28

    init {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")
        ethereumKit = EthereumKit(words, NetworkType.Ropsten)
        ethereumKit.include(contractAddress, decimal)

        ethereumKit.listener = this

        ethereumKit.transactions().subscribe { txList: List<Transaction> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
        }

        balance.value = ethereumKit.balance
        balanceToken.value = ethereumKit.balanceERC20[contractAddress] ?: 0.0
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

    //
    // ERC20
    //
    fun sendERC20(address: String, amount: Double) {
        ethereumKit.sendERC20(address, contractAddress, decimal, amount) { error ->
            sendStatus.value = error
        }
    }

    fun filterTransactions(ethTx: Boolean) {
        val txMethod = if (ethTx)
            ethereumKit.transactions() else
            ethereumKit.transactionsERC20(contractAddress)

        txMethod.subscribe { txList: List<Transaction> ->
            transactions.value = txList
        }.let {
            disposables.add(it)
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
        if (address == contractAddress) {
            this.balanceToken.postValue(balance)
        } else {
            this.balance.postValue(balance)
        }
    }

    override fun lastBlockHeightUpdated(height: Int) {
        this.lastBlockHeight.postValue(height)
    }

    override fun onKitStateUpdate(contractAddress: String?, state: KitState) {
        contractAddress?.let { return }

        this.kitState.postValue(state)
    }
}
