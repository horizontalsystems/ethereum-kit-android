package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.models.NetworkType
import io.horizontalsystems.ethereumkit.network.Configuration
import io.horizontalsystems.hdwalletkit.HDWallet
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function3
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class ApiBlockchain(
        private val storage: IStorage,
        private val apiProvider: IApiProvider,
        override val ethereumAddress: String) : IBlockchain {

    private var erc20Contracts = HashMap<String, Erc20Contract>()
    private val disposables = CompositeDisposable()

    override var gasPriceInWei: Long = 10_000_000_000
        private set
    override val gasLimitEthereum: Int = 21_000
    override val gasLimitErc20: Int = 100_000

    override var listener: IBlockchainListener? = null

    override val blockchainSyncState: EthereumKit.SyncState
        get() { return syncState }

    private val refreshInterval: Long = 30

    private var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
        set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateState(value)
            }
        }

    init {
        storage.getGasPriceInWei()?.let {
            gasPriceInWei = it
        }

        Flowable.interval(refreshInterval, TimeUnit.SECONDS)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    refreshAll()
                }?.let { disposables.add(it) }
    }

    override fun start() {
        refreshAll()
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

    override fun register(contractAddress: String, decimal: Int) {
        if (erc20Contracts[contractAddress] != null) {
            return
        }

        erc20Contracts[contractAddress] = (Erc20Contract(contractAddress, decimal, EthereumKit.SyncState.NotSynced))

        refreshAll()
    }

    override fun unregister(contractAddress: String) {
        erc20Contracts.remove(contractAddress)
    }

    override fun send(toAddress: String, amount: BigDecimal, gasPriceInWei: Long?): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(ethereumAddress)
                .flatMap { nonce ->
                    apiProvider.send(ethereumAddress, toAddress, nonce, amount, gasPriceInWei
                            ?: this.gasPriceInWei, gasLimitEthereum)
                }
                .doAfterSuccess {
                    updateTransactions(listOf(it))
                }
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: BigDecimal, gasPriceInWei: Long?): Single<EthereumTransaction> {
        val contract = erc20Contracts[contractAddress]
                ?: return Single.error(ApiException.ContractNotRegistered)

        return apiProvider.getTransactionCount(ethereumAddress)
                .flatMap { nonce ->
                    apiProvider.sendErc20(contractAddress, contract.decimal, ethereumAddress, toAddress, nonce, amount, gasPriceInWei
                            ?: this.gasPriceInWei, gasLimitErc20)
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
            if (it.syncState == EthereumKit.SyncState.Syncing){
                return
            }
        }

        changeAllSyncStates(EthereumKit.SyncState.Syncing)

        Single.zip(
                apiProvider.getLastBlockHeight(),
                apiProvider.getGasPriceInWei(),
                apiProvider.getBalance(ethereumAddress),
                Function3<Int, Long, BigDecimal, Triple<Int, Long, BigDecimal>> { t1, t2, t3 ->
                    Triple(t1, t2, t3)
                })
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .subscribe({ result ->
                    updateLastBlockHeight(result.first)
                    updateGasPrice(result.second)
                    updateBalance(result.third)

                    refreshTransactions()
                }, {
                    it?.printStackTrace()
                    changeAllSyncStates(EthereumKit.SyncState.NotSynced)
                }).let {
                    disposables.add(it)
                }

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
        val decimals = hashMapOf<String, Int>()
        erc20Contracts.values.forEach {
            decimals[it.address] = it.decimal
        }

        apiProvider.getTransactionsErc20(ethereumAddress, (erc20LastTransactionBlockHeight + 1), decimals)
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
        erc20Contracts.values.forEach {contract ->
            apiProvider.getBalanceErc20(ethereumAddress, contract.address, contract.decimal)
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

    private fun updateGasPrice(gasPriceInWei: Long) {
        this.gasPriceInWei = gasPriceInWei
        storage.saveGasPriceInWei(gasPriceInWei)
    }

    private fun updateBalance(balance: BigDecimal) {
        storage.saveBalance(balance, ethereumAddress)
        listener?.onUpdateBalance(balance)
    }

    private fun updateErc20Balance(balance: BigDecimal, contractAddress: String) {
        storage.saveBalance(balance, contractAddress)
        listener?.onUpdateErc20Balance(balance, contractAddress)
    }

    private fun updateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions)
    }

    private fun updateTransactionsErc20(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)

        val contractTransactions = HashMap<String, MutableList<EthereumTransaction>>()

        ethereumTransactions.forEach { transaction ->
            val address = transaction.contractAddress
            if (contractTransactions[address] == null){
                contractTransactions[address] = mutableListOf()
            }
            contractTransactions[address]?.add(transaction)
        }

        contractTransactions.forEach { (contractAddress, transactions) ->
            listener?.onUpdateErc20Transactions(transactions, contractAddress)
        }
    }

    class Erc20Contract(var address: String, var decimal: Int, var syncState: EthereumKit.SyncState)

    sealed class ApiException : Exception() {
        object ContractNotRegistered : ApiException()
        object InternalException : ApiException()
    }

    companion object {
        fun apiBlockchain(storage: IStorage, seed: ByteArray, testMode: Boolean, infuraKey: String, etherscanKey: String, debugPrints: Boolean = false): ApiBlockchain {

            val hdWallet = HDWallet(seed, if (testMode) 1 else 60)

            val networkType: NetworkType = if (testMode) NetworkType.Ropsten else NetworkType.MainNet

            val configuration = Configuration(
                    networkType = networkType,
                    infuraKey = infuraKey,
                    debugPrints = debugPrints,
                    etherscanAPIKey = etherscanKey
            )

            val apiProvider = ApiProvider(configuration, hdWallet)

            return ApiBlockchain(storage, apiProvider, hdWallet.address())
        }
    }

}
