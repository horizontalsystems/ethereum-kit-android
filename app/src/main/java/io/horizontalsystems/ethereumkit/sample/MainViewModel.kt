package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.EthereumKit.KitState
import io.horizontalsystems.ethereumkit.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.disposables.CompositeDisposable

class MainViewModel : ViewModel() {

    val transactions = MutableLiveData<List<Transaction>>()
    val balance = MutableLiveData<Double>()
    val fee = MutableLiveData<Double>()
    val lastBlockHeight = MutableLiveData<Int>()
    val kitState = MutableLiveData<KitState>()

    val tokenTransactions = MutableLiveData<List<Transaction>>()
    val tokenBalance = MutableLiveData<Double>()

    val sendStatus = SingleLiveEvent<Throwable?>()

    private val disposables = CompositeDisposable()

    private var ethereumKit: EthereumKit
    private val contractAddress = "0xF559862f9265756619d5523bBC4bd8422898e97d"

    private val erc20Adapter = ERC20Adapter(contractAddress, 28)
    private val ethereumAdapter = EthereumAdapter()

    init {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

        ethereumKit = EthereumKit(words, NetworkType.Ropsten, "unique-wallet-id")
        ethereumKit.listener = ethereumAdapter
        ethereumKit.register(erc20Adapter)

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
            this.balance.postValue(it)
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.lastBlockHeightSubject.subscribe {
            this.lastBlockHeight.postValue(it)
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.kitStateUpdateSubject.subscribe {
            this.kitState.postValue(it)
        }.let {
            disposables.add(it)
        }

        //
        // ERC20
        //
        erc20Adapter.balanceSubject.subscribe {
            this.tokenBalance.postValue(it)
        }.let {
            disposables.add(it)
        }

        ethereumKit.start()
    }

    //
    // Ethereum
    //

    fun refresh() {
        ethereumKit.refresh()
        fee.postValue(ethereumKit.fee())
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress
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
        ethereumKit.sendERC20(address, contractAddress, amount) { error ->
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

    //
    // Private
    //

    private fun updateTransactions() {
        ethereumKit.transactions().subscribe { list: List<Transaction> ->
            transactions.value = list
        }.let {
            disposables.add(it)
        }
    }

}
