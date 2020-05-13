package io.horizontalsystems.ethereumkit.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
import java.util.logging.Logger

class MainViewModel : ViewModel() {

    private val decimal = 18

    private val logger = Logger.getLogger("MainViewModel")

    private val infuraCredentials = EthereumKit.InfuraCredentials(
            projectId = "2a1306f1d12f4c109a4d4fb9be46b02e",
            secretKey = "fc479a9290b64a84a15fa6544a130218")
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val contractAddress = "0x4f3AfEC4E5a3F2A6a1A411DEF7D7dFe50eE057bF"
    private val contractDecimal = 9
    private val networkType: NetworkType = NetworkType.Ropsten
    private val walletId = "walletId"
    private var estimateGasLimit: Long = 0

    private val disposables = CompositeDisposable()

    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter

    private lateinit var erc20Adapter: Erc20Adapter

    val transactions = MutableLiveData<List<TransactionRecord>>()
    val balance = MutableLiveData<BigDecimal>()
    val fee = MutableLiveData<BigDecimal>()
    val lastBlockHeight = MutableLiveData<Long>()
    val syncState = MutableLiveData<SyncState>()
    val transactionsSyncState = MutableLiveData<SyncState>()
    val erc20SyncState = MutableLiveData<SyncState>()
    val erc20TransactionsSyncState = MutableLiveData<SyncState>()

    val erc20TokenBalance = MutableLiveData<BigDecimal>()
    val sendStatus = SingleLiveEvent<Throwable?>()
    val estimateGas = SingleLiveEvent<String>()


    val gasPrice: Long = 5_000_000_000


    fun init() {
        val words = "mom year father track attend frown loyal goddess crisp abandon juice roof".split(" ")

        val seed = Mnemonic().toSeed(words)
        val hdWallet = HDWallet(seed, if (networkType == NetworkType.MainNet) 60 else 1)
        val privateKey = hdWallet.privateKey(0, 0, true).privKey
        val nodePrivateKey = hdWallet.privateKey(102, 102, true).privKey
        val rpcApi = EthereumKit.RpcApi.Incubed()

        ethereumKit = EthereumKit.getInstance(App.instance, privateKey, EthereumKit.SyncMode.ApiSyncMode(), networkType, rpcApi, etherscanKey, walletId)
        ethereumAdapter = EthereumAdapter(ethereumKit)

        erc20Adapter = Erc20Adapter(App.instance, ethereumKit, "Max Token", "MXT", contractAddress, contractDecimal)

        fee.value = ethereumKit.fee(gasPrice)
        updateBalance()
        updateErc20Balance()
        updateState()
        updateTransactionsSyncState()
        updateErc20State()
        updateErc20TransactionsSyncState()
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

        ethereumAdapter.transactionsSyncStateFlowable.subscribe {
            updateTransactionsSyncState()
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

        erc20Adapter.transactionsSyncStateFlowable.subscribe {
            updateErc20TransactionsSyncState()
        }.let {
            disposables.add(it)
        }

        ethereumKit.start()
        erc20Adapter.refresh()
    }

    private fun updateLastBlockHeight() {
        lastBlockHeight.postValue(ethereumKit.lastBlockHeight)
    }

    private fun updateState() {
        syncState.postValue(ethereumAdapter.syncState)
    }

    private fun updateTransactionsSyncState() {
        transactionsSyncState.postValue(ethereumAdapter.transactionsSyncState)
    }

    private fun updateErc20State() {
        erc20SyncState.postValue(erc20Adapter.syncState)
    }

    private fun updateErc20TransactionsSyncState() {
        erc20TransactionsSyncState.postValue(erc20Adapter.transactionsSyncState)
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
        ethereumAdapter.refresh()
        erc20Adapter.refresh()
        fee.postValue(ethereumKit.fee(gasPrice))
    }

    fun clear() {
        EthereumKit.clear(App.instance, networkType, walletId)
        Erc20Kit.clear(App.instance, networkType, walletId)
        init()
    }

    fun receiveAddress(): String {
        return ethereumKit.receiveAddress
    }

    fun estimageGas(toAddress: String, value: BigDecimal): Boolean {

        val poweredDecimal = value.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0)

        return ethereumAdapter.estimatedGasLimit(toAddress = toAddress, value = noScaleDecimal)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    estimateGas.value = it.toString()
                    estimateGasLimit = it
                }, {
                    logger.warning("Gas estimate: ${it.message}")
                    estimateGas.value = "Gas Estimate:Error"
                }).let { disposables.add(it) }

    }

    fun estimateERC20Gas(toAddress: String, value: BigDecimal): Boolean {

        return erc20Adapter.estimatedGasLimit(toAddress = toAddress, value = value)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    estimateGas.value = it.toString()
                    estimateGasLimit = it
                }, {
                    logger.warning("Gas estimate: ${it.message}")
                    estimateGas.value = "Gas Estimate:Error"
                }).let { disposables.add(it) }

    }

    fun send(toAddress: String, amount: BigDecimal) {
        ethereumAdapter.send(address = toAddress, amount = amount, gasLimit = estimateGasLimit)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    sendStatus.value = null
                }, {
                    logger.warning("Ether send failed: ${it.message}")
                    sendStatus.value = it
                }).let { disposables.add(it) }

    }

    //
    // ERC20
    //

    fun sendERC20(toAddress: String, amount: BigDecimal) {
        erc20Adapter.send(toAddress, amount, estimateGasLimit)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    //success
                    sendStatus.value = null
                }, {
                    logger.warning("Erc20 send failed: ${it.message}")
                    sendStatus.value = it
                }).let { disposables.add(it) }
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
