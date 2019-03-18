package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import android.widget.Toast
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.FeePriority
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumAdapter
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MainViewModel : ViewModel() {

    private val infuraKey = "2a1306f1d12f4c109a4d4fb9be46b02e"
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val contractAddress = "0xF559862f9265756619d5523bBC4bd8422898e97d"
    private val contractDecimal = 28
    private val testMode = true

    private val disposables = CompositeDisposable()

    private var ethereumKit: EthereumKit
    private val erc20Adapter: Erc20Adapter
    private val ethereumAdapter: EthereumAdapter
    var feePriority: FeePriority  = FeePriority.Medium



    val transactions = MutableLiveData<List<TransactionRecord>>()
    val balance = MutableLiveData<BigDecimal>()
    val fee = MutableLiveData<BigDecimal>()
    val lastBlockHeight = MutableLiveData<Int>()
    val etherState = MutableLiveData<SyncState>()
    val erc20State = MutableLiveData<SyncState>()

    val erc20TokenBalance = MutableLiveData<BigDecimal>()
    val sendStatus = SingleLiveEvent<Throwable?>()


    init {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

        ethereumKit = EthereumKit.ethereumKit(App.instance, words, "unique-wallet-id", testMode, infuraKey = infuraKey, etherscanKey = etherscanKey)
        ethereumAdapter = EthereumAdapter(ethereumKit)
        erc20Adapter = Erc20Adapter(ethereumKit, contractAddress, contractDecimal)

        ethereumKit.start()


        fee.value = ethereumKit.fee()
        updateBalance()
        updateErc20Balance()
        updateState()
        updateErc20State()
        updateLastBlockHeight()

        //
        // Ethereum
        //
        ethereumAdapter.transactionSubject.subscribe {
            updateTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.balanceSubject.subscribe {
            updateBalance()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.lastBlockHeightSubject.subscribe {
            updateLastBlockHeight()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.syncStateUpdateSubject.subscribe {
            updateState()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.syncStateUpdateSubject.subscribe {
            updateErc20State()
        }.let {
            disposables.add(it)
        }

        //
        // ERC20
        //

        erc20Adapter.balanceSubject.subscribe {
            updateErc20Balance()
        }.let {
            disposables.add(it)
        }

        ethereumKit.start()
    }

    private fun updateLastBlockHeight() {
        lastBlockHeight.postValue(ethereumKit.lastBlockHeight)
    }

    private fun updateState() {
        etherState.postValue(ethereumAdapter.syncState)
    }

    private fun updateErc20State() {
        erc20State.postValue(erc20Adapter.syncState)
    }

    private fun updateBalance() {
        balance.postValue(ethereumAdapter.balance)
    }

    private fun updateErc20Balance() {
        erc20TokenBalance.postValue(erc20Adapter.balance)
    }

    //
    // Ethereum
    //

    fun refresh() {
        ethereumKit.start()
        fee.postValue(ethereumKit.fee())
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress
    }

    fun send(address: String, amount: BigDecimal) {
        ethereumAdapter.sendSingle(address, amount, feePriority)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }

    }

    //
    // ERC20
    //

    fun sendERC20(address: String, amount: BigDecimal) {
        erc20Adapter.sendSingle(address, amount, feePriority)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    Toast.makeText(App.instance, "Success", Toast.LENGTH_SHORT).show()
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }
    }

    fun filterTransactions(ethTx: Boolean) {
        val txMethod = if (ethTx) ethereumAdapter.transactionsSingle() else erc20Adapter.transactionsSingle()

        txMethod
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { txList: List<TransactionRecord> ->
                    transactions.value = txList
                }.let {
                    disposables.add(it)
                }
    }

    //
    // Private
    //

    private fun updateTransactions() {
        ethereumAdapter.transactionsSingle()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

}
