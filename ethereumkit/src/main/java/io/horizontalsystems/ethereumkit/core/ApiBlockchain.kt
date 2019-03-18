package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.FeePriority
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.NetworkType
import io.horizontalsystems.ethereumkit.network.ApiGasPrice
import io.horizontalsystems.ethereumkit.network.Configuration
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.Keys
import java.util.concurrent.TimeUnit

class ApiBlockchain(
        private val storage: IApiStorage,
        private val apiProvider: IApiProvider,
        override val ethereumAddress: String) : IBlockchain {

    private var erc20Contracts = HashMap<String, Erc20Contract>()
    private val disposables = CompositeDisposable()

    override var gasPriceData: GasPrice = GasPrice.defaultGasPrice
        private set

    override val gasLimitEthereum: Int = 21_000
    override val gasLimitErc20: Int = 100_000

    override var listener: IBlockchainListener? = null

    override val blockchainSyncState: EthereumKit.SyncState
        get() = syncState

    private val refreshInterval: Long = 30
    private val gasPriceRefreshInterval: Long = 180

    private var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
        set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateState(value)
            }
        }

    private val apiGasPrice = ApiGasPrice()

    init {
        storage.getGasPriceInWei()?.let {
            gasPriceData = it
        }

        Flowable.interval(refreshInterval, TimeUnit.SECONDS)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    refreshAll()
                }?.let { disposables.add(it) }

        Flowable.interval(gasPriceRefreshInterval, TimeUnit.SECONDS)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    refreshGasPrice()
                }?.let { disposables.add(it) }
    }

    override fun start() {
        refreshAll()
        refreshGasPrice()
    }

    override fun stop() {
        disposables.clear()
    }

    override fun clear() {
        erc20Contracts.clear()
        disposables.clear()
    }

    override fun syncState(contractAddress: String): EthereumKit.SyncState {
        return erc20Contracts[contractAddress]?.syncState ?: EthereumKit.SyncState.NotSynced
    }

    override fun register(contractAddress: String) {
        if (erc20Contracts[contractAddress] != null) {
            return
        }

        erc20Contracts[contractAddress] = (Erc20Contract(contractAddress, EthereumKit.SyncState.NotSynced))

        refreshAll()
    }

    override fun unregister(contractAddress: String) {
        erc20Contracts.remove(contractAddress)
    }

    override fun send(toAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(ethereumAddress)
                .flatMap { nonce ->
                    apiProvider.send(ethereumAddress, toAddress, nonce, amount, gasPriceInWei(feePriority), gasLimitEthereum)
                }
                .doAfterSuccess {
                    updateTransactions(listOf(it))
                }
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: String, feePriority: FeePriority): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(ethereumAddress)
                .flatMap { nonce ->
                    apiProvider.sendErc20(contractAddress, ethereumAddress, toAddress, nonce, amount, gasPriceInWei(feePriority), gasLimitErc20)
                }
                .doAfterSuccess {
                    updateTransactionsErc20(listOf(it))
                }
    }

    private fun refreshAll() {
        if (syncState == EthereumKit.SyncState.Syncing) {
            return
        }
        erc20Contracts.values.forEach {
            if (it.syncState == EthereumKit.SyncState.Syncing) {
                return
            }
        }

        changeAllSyncStates(EthereumKit.SyncState.Syncing)

        Single.zip(
                apiProvider.getLastBlockHeight(),
                apiProvider.getBalance(ethereumAddress),
                BiFunction<Int, String, Pair<Int, String>> { t1, t2 -> Pair(t1, t2) })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({ result ->
                    updateLastBlockHeight(result.first)
                    updateBalance(result.second)
                    refreshTransactions()
                }, {
                    it?.printStackTrace()
                    changeAllSyncStates(EthereumKit.SyncState.NotSynced)
                }).let {
                    disposables.add(it)
                }

    }

    override fun gasPriceInWei(feePriority: FeePriority): Long {
        return when(feePriority) {
            FeePriority.LOWEST -> gasPriceData.lowPriority
            FeePriority.LOW -> {
                (gasPriceData.lowPriority + gasPriceData.mediumPriority) / 2
            }
            FeePriority.MEDIUM -> gasPriceData.mediumPriority
            FeePriority.HIGH -> {
                (gasPriceData.mediumPriority + gasPriceData.highPriority) / 2
            }
            FeePriority.HIGHEST -> gasPriceData.highPriority
        }
    }

    private fun refreshGasPrice() {
        apiGasPrice.getGasPrice()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    updateGasPrice(it)
                }, {
                    //error
                })?.let { disposables.add(it) }
    }

    private fun refreshTransactions() {
        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight(false) ?: 0

        apiProvider.getTransactions(ethereumAddress, (lastTransactionBlockHeight + 1))
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({ transactions ->
                    updateTransactions(transactions)
                    syncState = EthereumKit.SyncState.Synced
                }, {
                    syncState = EthereumKit.SyncState.NotSynced
                })?.let {
                    disposables.add(it)
                }

        if (erc20Contracts.isEmpty()) {
            return
        }

        val erc20LastTransactionBlockHeight = storage.getLastTransactionBlockHeight(true) ?: 0

        apiProvider.getTransactionsErc20(ethereumAddress, (erc20LastTransactionBlockHeight + 1))
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({ transactions ->
                    updateTransactionsErc20(transactions)
                    refreshErc20Balances()
                }, {
                    erc20Contracts.values.forEach {
                        updateSyncState(EthereumKit.SyncState.NotSynced, it.address)
                    }
                })?.let {
                    disposables.add(it)
                }
    }

    private fun refreshErc20Balances() {
        erc20Contracts.values.forEach { contract ->
            apiProvider.getBalanceErc20(ethereumAddress, contract.address)
                    .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                    .subscribe({ balance ->
                        updateErc20Balance(balance, contract.address)
                        updateSyncState(EthereumKit.SyncState.Synced, contract.address)
                    }, {
                        updateSyncState(EthereumKit.SyncState.NotSynced, contract.address)
                    })?.let {
                        disposables.add(it)
                    }
        }
    }

    private fun changeAllSyncStates(state: EthereumKit.SyncState) {
        syncState = state
        erc20Contracts.values.forEach {
            updateSyncState(state, it.address)
        }
    }

    private fun updateSyncState(syncState: EthereumKit.SyncState, contractAddress: String) {
        if (erc20Contracts[contractAddress]?.syncState == syncState) {
            return
        }

        erc20Contracts[contractAddress]?.syncState = syncState
        listener?.onUpdateErc20State(syncState, contractAddress)
    }

    private fun updateLastBlockHeight(height: Int) {
        storage.saveLastBlockHeight(height)
        listener?.onUpdateLastBlockHeight(height)
    }

    private fun updateGasPrice(gasPrice: GasPrice) {
        gasPriceData = gasPrice
        storage.saveGasPriceInWei(gasPrice)
    }

    private fun updateBalance(balance: String) {
        storage.saveBalance(balance, ethereumAddress)
        listener?.onUpdateBalance(balance)
    }

    private fun updateErc20Balance(balance: String, contractAddress: String) {
        storage.saveBalance(balance, contractAddress)
        listener?.onUpdateErc20Balance(balance, contractAddress)
    }

    private fun updateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions.filter { it.input == "0x" })
    }

    private fun updateTransactionsErc20(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)

        val contractTransactions = HashMap<String, MutableList<EthereumTransaction>>()

        ethereumTransactions.forEach { transaction ->
            val address = transaction.contractAddress
            if (contractTransactions[address] == null) {
                contractTransactions[address] = mutableListOf()
            }
            contractTransactions[address]?.add(transaction)
        }

        contractTransactions.forEach { (contractAddress, transactions) ->
            if (erc20Contracts[contractAddress] != null) {
                listener?.onUpdateErc20Transactions(transactions, contractAddress)
            }
        }
    }

    class Erc20Contract(var address: String, var syncState: EthereumKit.SyncState)

    sealed class ApiException : Exception() {
        object ContractNotRegistered : ApiException()
        object InternalException : ApiException()
    }

    companion object {
        fun apiBlockchain(storage: IApiStorage, seed: ByteArray, testMode: Boolean, infuraKey: String, etherscanKey: String, debugPrints: Boolean = false): ApiBlockchain {

            val hdWallet = HDWallet(seed, if (testMode) 1 else 60)

            val networkType: NetworkType = if (testMode) NetworkType.Ropsten else NetworkType.MainNet

            val configuration = Configuration(
                    networkType = networkType,
                    infuraKey = infuraKey,
                    debugPrints = debugPrints,
                    etherscanAPIKey = etherscanKey
            )

            val apiProvider = ApiProvider(configuration, hdWallet)

            val formattedAddress = Keys.toChecksumAddress(hdWallet.address())

            return ApiBlockchain(storage, apiProvider, formattedAddress)
        }
    }

}
