package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class MainViewModel : ViewModel() {

    val transactions = MutableLiveData<List<EthereumTransaction>>()
    val balance = MutableLiveData<BigDecimal>()
    val fee = MutableLiveData<BigDecimal>()
    val lastBlockHeight = MutableLiveData<Int>()
    val kitState = MutableLiveData<SyncState>()

    val tokenBalance = MutableLiveData<BigDecimal>()

    val sendStatus = SingleLiveEvent<Throwable?>()

    private val disposables = CompositeDisposable()

    private var ethereumKit: EthereumKit
    private val contractAddress = "0xF559862f9265756619d5523bBC4bd8422898e97d"

    private val erc20Adapter = ERC20Adapter(contractAddress, 28)
    private val ethereumAdapter = EthereumAdapter()

    init {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

        ethereumKit = EthereumKit.ethereumKit(App.instance, words, "unique-wallet-id", true, infuraKey = "2a1306f1d12f4c109a4d4fb9be46b02e", etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE")
        ethereumKit.listener = ethereumAdapter
        ethereumKit.register(contractAddress, 28, erc20Adapter)

        // Previous or default values
        balance.value = ethereumKit.balance
        tokenBalance.value = ethereumKit.balanceERC20(contractAddress)
        fee.value = ethereumKit.fee()

        //
        // Ethereum
        //
        ethereumAdapter.transactionSubject.subscribe {
            updateTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.balanceSubject.subscribe {
            this.balance.postValue(ethereumKit.balance)
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.lastBlockHeightSubject.subscribe {
            this.lastBlockHeight.postValue(ethereumKit.lastBlockHeight)
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.kitStateUpdateSubject.subscribe {
            this.kitState.postValue(ethereumKit.syncState)
        }.let {
            disposables.add(it)
        }

        //
        // ERC20
        //

        erc20Adapter.balanceSubject.subscribe {
            this.tokenBalance.postValue(ethereumKit.balanceERC20(contractAddress))
        }.let {
            disposables.add(it)
        }

        ethereumKit.start()
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
        ethereumKit.send(address, amount)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: EthereumTransaction? ->
                    //success
                    Log.e("MainViewModel", "send success")
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }
    }

    //
    // ERC20
    //

    fun sendERC20(address: String, amount: BigDecimal) {
        ethereumKit.sendERC20(address, contractAddress, amount)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ t: EthereumTransaction? ->
                    //success
                    Log.e("MainViewModel", "send success")
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }
    }

    fun filterTransactions(ethTx: Boolean) {
        val txMethod = if (ethTx)
            ethereumKit.transactions() else
            ethereumKit.transactionsERC20(contractAddress)

        txMethod
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { txList: List<EthereumTransaction> ->
                    transactions.value = txList
                }.let {
                    disposables.add(it)
                }
    }

    //
    // Private
    //

    private fun updateTransactions() {
        ethereumKit.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<EthereumTransaction> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

}
