package io.horizontalsystems.ethereumkit.sample.modules.addresswatch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.sample.App
import io.horizontalsystems.ethereumkit.sample.Configuration
import io.horizontalsystems.ethereumkit.sample.SingleLiveEvent
import io.horizontalsystems.ethereumkit.sample.core.Erc20BaseAdapter
import io.horizontalsystems.ethereumkit.sample.core.EthereumBaseAdapter
import io.horizontalsystems.ethereumkit.sample.core.TransactionRecord
import io.horizontalsystems.ethereumkit.sample.modules.main.ShowTxType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class AddressWatchViewModel : ViewModel() {

    private val disposables = CompositeDisposable()

    private var showTxType = ShowTxType.Eth
    private var ethTxs = listOf<TransactionRecord>()
    private var erc20Txs = listOf<TransactionRecord>()
    private var ethTxSyncState: EthereumKit.SyncState = EthereumKit.SyncState.Synced()
    private var erc20TxSyncState: EthereumKit.SyncState = EthereumKit.SyncState.Synced()

    val lastBlockHeight = MutableLiveData<Long>()
    val transactions = MutableLiveData<List<TransactionRecord>>()
    val showWarningLiveEvent = SingleLiveEvent<String>()
    val showTxTypeLiveData = MutableLiveData<ShowTxType>()
    val transactionsSyncingLiveData = MutableLiveData(false)


    fun watchAddress(words: String) {
        if (words.isBlank()) {
            showWarningLiveEvent.postValue("Enter words first")
            return
        }

        val wordList: List<String> = words.trim().split(" ")

        if (wordList.size != 12 && wordList.size != 24) {
            showWarningLiveEvent.postValue("Check entered words. Number of words is ${wordList.size}")
            return
        }

        clearKits()

        val evmKit = createKit(wordList)
        val evmAdapter = EthereumBaseAdapter(evmKit)
        val erc20Adapter = Erc20BaseAdapter(App.instance, Configuration.erc20Tokens.first(), evmKit)

        Erc20Kit.addTransactionSyncer(evmKit)
        Erc20Kit.addDecorator(evmKit)

        evmAdapter.lastBlockHeightFlowable.subscribe {
            lastBlockHeight.postValue(evmKit.lastBlockHeight)
            updateEthTransactions(evmAdapter)
        }.let {
            disposables.add(it)
        }

        evmAdapter.transactionsFlowable.subscribe {
            updateEthTransactions(evmAdapter)
        }.let {
            disposables.add(it)
        }

        erc20Adapter.transactionsFlowable.subscribe {
            updateErc20Transactions(erc20Adapter)
        }.let {
            disposables.add(it)
        }

        evmAdapter.transactionsSyncStateFlowable.subscribe {
            ethTxSyncState = evmAdapter.transactionsSyncState
            updateTransactionsSyncState()
        }.let {
            disposables.add(it)
        }

        erc20Adapter.transactionsSyncStateFlowable.subscribe {
            erc20TxSyncState = erc20Adapter.transactionsSyncState
            updateTransactionsSyncState()
        }.let {
            disposables.add(it)
        }

        evmAdapter.start()
        erc20Adapter.start()
    }

    override fun onCleared() {
        clearKits()
        disposables.clear()
    }

    private fun clearKits() {
        EthereumKit.clear(App.instance, Configuration.chain, Configuration.walletId)
        Erc20Kit.clear(App.instance, Configuration.chain, Configuration.walletId)
    }

    private fun updateTransactionsSyncState() {
        var syncing = false
        if (ethTxSyncState is EthereumKit.SyncState.Syncing || erc20TxSyncState is EthereumKit.SyncState.Syncing) {
            syncing = true
        }
        transactionsSyncingLiveData.postValue(syncing)
    }

    fun filterTransactions(ethTx: Boolean) {
        if (ethTx) {
            showTxType = ShowTxType.Eth
        } else {
            showTxType = ShowTxType.Erc20
        }
        showTxTypeLiveData.postValue(showTxType)
        updateTransactionList()
    }

    private fun createKit(wordList: List<String>): EthereumKit {
        val syncSource: EthereumKit.SyncSource?
        val txApiProviderKey: String

        when (Configuration.chain.id) {
            56 -> {
                txApiProviderKey = Configuration.bscScanKey
                syncSource = if (Configuration.webSocket)
                    EthereumKit.defaultBscWebSocketSyncSource()
                else
                    EthereumKit.defaultBscHttpSyncSource()
            }
            else -> {
                txApiProviderKey = Configuration.etherscanKey
                syncSource = if (Configuration.webSocket)
                    EthereumKit.infuraWebSocketSyncSource(
                        Configuration.chain,
                        Configuration.infuraProjectId,
                        Configuration.infuraSecret
                    )
                else
                    EthereumKit.infuraHttpSyncSource(
                        Configuration.chain,
                        Configuration.infuraProjectId,
                        Configuration.infuraSecret
                    )
            }
        }
        checkNotNull(syncSource) {
            throw Exception("Could not get syncSource!")
        }

        return EthereumKit.getInstance(
            App.instance, wordList, "",
            Configuration.chain, syncSource, txApiProviderKey,
            Configuration.walletId
        )
    }

    private fun updateEthTransactions(evmAdapter: EthereumBaseAdapter) {
        evmAdapter.transactions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: List<TransactionRecord> ->
                ethTxs = list
                updateTransactionList()
            }.let {
                disposables.add(it)
            }
    }

    private fun updateErc20Transactions(erc20Adapter: Erc20BaseAdapter) {
        erc20Adapter.transactions()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { list: List<TransactionRecord> ->
                erc20Txs = list
                updateTransactionList()
            }.let {
                disposables.add(it)
            }
    }

    private fun updateTransactionList() {
        val list = when (showTxType) {
            ShowTxType.Eth -> ethTxs
            ShowTxType.Erc20 -> erc20Txs
        }
        transactions.value = list
    }

}
