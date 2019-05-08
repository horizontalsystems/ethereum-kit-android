package io.horizontalsystems.ethereumkit.sample

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.util.Log
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.sample.core.Erc20Adapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumAdapter
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
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

    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter

    private lateinit var erc20Adapter: Erc20Adapter

    val transactions = MutableLiveData<List<TransactionRecord>>()
    val balance = MutableLiveData<BigDecimal>()
    val fee = MutableLiveData<BigDecimal>()
    val lastBlockHeight = MutableLiveData<Long>()
    val etherState = MutableLiveData<SyncState>()
    val erc20State = MutableLiveData<SyncState>()

    val erc20TokenBalance = MutableLiveData<BigDecimal>()
    val sendStatus = SingleLiveEvent<Throwable?>()


    val gasPrice: Long = 5_000_000_000

    init {
        init()
    }

    fun init() {
        //  val words = "subway plate brick pattern inform used oblige identify cherry drop flush balance".split(" ")
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

        val seed = Mnemonic().toSeed(words)
        val hdWallet = HDWallet(seed, if (testMode) 1 else 60)
        val privateKey = hdWallet.privateKey(0, 0, true).privKey
        val nodePrivateKey = hdWallet.privateKey(102, 102, true).privKey

        ethereumKit = EthereumKit.getInstance(App.instance, privateKey, EthereumKit.SyncMode.ApiSyncMode(infuraKey, etherscanKey), NetworkType.Ropsten, "unique-wallet-id")
        ethereumAdapter = EthereumAdapter(ethereumKit)

        erc20Adapter = Erc20Adapter(App.instance, ethereumKit, "Max Token", "MXT", contractAddress, contractDecimal)

        fee.value = ethereumKit.fee(gasPrice)
        updateBalance()
        updateErc20Balance()
        updateState()
        updateErc20State()
        updateLastBlockHeight()

        //
        // Ethereum
        //

        ethereumAdapter.lastBlockHeightFlowable.subscribe {
            updateLastBlockHeight()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.transactionsFlowable.subscribe {
            updateTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.balanceFlowable.subscribe {
            updateBalance()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.syncStateFlowable.subscribe {
            updateState()
        }.let {
            disposables.add(it)
        }


        //
        // ERC20
        //

        erc20Adapter.transactionsFlowable.subscribe {
            updateErc20Transactions()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.balanceFlowable.subscribe {
            updateErc20Balance()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.syncStateFlowable.subscribe {
            updateErc20State()
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

    private fun updateTransactions() {
        ethereumAdapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

    private fun updateErc20Transactions() {
        erc20Adapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }


    //
    // Ethereum
    //

    fun refresh() {
        ethereumKit.refresh()
        fee.postValue(ethereumKit.fee(gasPrice))
    }

    fun clear() {
        EthereumKit.clear(App.instance)
        Erc20Kit.clear(App.instance)
        init()
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress
    }

    fun send(address: String, amount: BigDecimal) {
        ethereumAdapter.send(address, amount)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    sendStatus.value = null
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }

    }

    //
    // ERC20
    //

    fun sendERC20(address: String, amount: BigDecimal) {
        erc20Adapter.send(address, amount)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    sendStatus.value = null
                }, {
                    Log.e("MainViewModel", "send failed ${it.message}")
                    sendStatus.value = it
                })?.let { disposables.add(it) }
    }

    fun filterTransactions(ethTx: Boolean) {
        val txMethod = if (ethTx) ethereumAdapter.transactions() else erc20Adapter.transactions()

        txMethod
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { txList: List<TransactionRecord> ->
                    transactions.value = txList
                }.let {
                    disposables.add(it)
                }
    }

}
