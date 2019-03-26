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
import io.reactivex.functions.BiFunction
import org.web3j.crypto.Keys
import java.util.concurrent.TimeUnit

class ApiBlockchain(
        private val storage: IApiStorage,
        private val apiProvider: IApiProvider,
        override val address: String) : IBlockchain {

    private var erc20Contracts = HashMap<String, Erc20Contract>()
    private val disposables = CompositeDisposable()

    override var listener: IBlockchainListener? = null

    override fun getLastBlockHeight(): Long? {
        return storage.getLastBlockHeight()
    }

    override val balance: String?
        get() = storage.getBalance(address)

    override fun getBalanceErc20(contractAddress: String): String? {
        return storage.getBalance(contractAddress)
    }

    override fun getTransactions(fromHash: String?, limit: Int?, contractAddress: String?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    private val refreshInterval: Long = 30

    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateState(value)
            }
        }

    init {
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
        storage.clear()
    }

    override fun syncStateErc20(contractAddress: String): EthereumKit.SyncState {
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

    override fun send(toAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(address)
                .flatMap { nonce ->
                    apiProvider.send(address, toAddress, nonce, amount, gasPrice, gasLimit)
                }
                .doAfterSuccess {
                    updateTransactions(listOf(it))
                }
    }

    override fun sendErc20(toAddress: String, contractAddress: String, amount: String, gasPrice: Long, gasLimit: Long): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(address)
                .flatMap { nonce ->
                    apiProvider.sendErc20(contractAddress, address, toAddress, nonce, amount, gasPrice, gasLimit)
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
                apiProvider.getBalance(address),
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

    private fun refreshTransactions() {
        val lastTransactionBlockHeight = storage.getLastTransactionBlockHeight(false) ?: 0

        apiProvider.getTransactions(address, (lastTransactionBlockHeight + 1))
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

        apiProvider.getTransactionsErc20(address, (erc20LastTransactionBlockHeight + 1))
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
            apiProvider.getBalanceErc20(address, contract.address)
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
        listener?.onUpdateLastBlockHeight(height.toLong())
    }

    private fun updateBalance(balance: String) {
        storage.saveBalance(balance, address)
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
