package io.horizontalsystems.ethereumkit.api

import io.horizontalsystems.ethereumkit.core.*
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class ApiBlockchain(
        private val storage: IApiStorage,
        private val apiProvider: IApiProvider,
        private val transactionSigner: TransactionSigner,
        private val transactionBuilder: TransactionBuilder,
        override val address: ByteArray
) : IBlockchain {

    private val refreshInterval: Long = 30
    private var erc20Contracts = HashMap<ByteArray, Erc20Contract>()
    private val disposables = CompositeDisposable()

    init {
        Flowable.interval(refreshInterval, TimeUnit.SECONDS)
                .subscribeOn(io.reactivex.schedulers.Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    refreshAll()
                }?.let { disposables.add(it) }
    }

    override var listener: IBlockchainListener? = null

    override fun getLastBlockHeight(): Long? {
        return storage.getLastBlockHeight()
    }

    override val balance: BigInteger?
        get() = storage.getBalance(address)

    override fun getBalanceErc20(contractAddress: ByteArray): BigInteger? {
        return storage.getBalance(contractAddress)
    }

    override fun getTransactions(fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, null)
    }

    override fun getTransactionsErc20(contractAddress: ByteArray?, fromHash: ByteArray?, limit: Int?): Single<List<EthereumTransaction>> {
        return storage.getTransactions(fromHash, limit, contractAddress)
    }

    override var syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced
        private set(value) {
            if (field != value) {
                field = value
                listener?.onUpdateSyncState(value)
            }
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

    override fun getSyncStateErc20(contractAddress: ByteArray): EthereumKit.SyncState {
        return erc20Contracts[contractAddress]?.syncState ?: EthereumKit.SyncState.NotSynced
    }

    override fun register(contractAddress: ByteArray) {
        if (erc20Contracts[contractAddress] != null) {
            return
        }

        erc20Contracts[contractAddress] = (Erc20Contract(contractAddress, EthereumKit.SyncState.NotSynced))

        refreshAll()
    }

    override fun unregister(contractAddress: ByteArray) {
        erc20Contracts.remove(contractAddress)
    }

    override fun send(rawTransaction: RawTransaction): Single<EthereumTransaction> {
        return apiProvider.getTransactionCount(address)
                .flatMap { nonce ->
                    send(rawTransaction, nonce)
                }.doOnSuccess { transaction ->
                    updateTransactions(listOf(transaction))
                }
    }

    private fun send(rawTransaction: RawTransaction, nonce: Long): Single<EthereumTransaction> {
        val signature = transactionSigner.sign(rawTransaction, nonce)
        val transaction = transactionBuilder.transaction(rawTransaction, nonce, signature, address)
        val encoded = transactionBuilder.encode(rawTransaction, nonce, signature)

        return apiProvider.send(signedTransaction = encoded)
                .map {
                    transaction
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
                BiFunction<Long, BigInteger, Pair<Long, BigInteger>> { t1, t2 -> Pair(t1, t2) })
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

    private fun updateSyncState(syncState: EthereumKit.SyncState, contractAddress: ByteArray) {
        if (erc20Contracts[contractAddress]?.syncState == syncState) {
            return
        }

        erc20Contracts[contractAddress]?.syncState = syncState
        listener?.onUpdateErc20SyncState(syncState, contractAddress)
    }

    private fun updateLastBlockHeight(height: Long) {
        storage.saveLastBlockHeight(height)
        listener?.onUpdateLastBlockHeight(height)
    }

    private fun updateBalance(balance: BigInteger) {
        storage.saveBalance(balance, address)
        listener?.onUpdateBalance(balance)
    }

    private fun updateErc20Balance(balance: BigInteger, contractAddress: ByteArray) {
        storage.saveBalance(balance, contractAddress)
        listener?.onUpdateErc20Balance(balance, contractAddress)
    }

    private fun updateTransactions(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)
        listener?.onUpdateTransactions(ethereumTransactions.filter { it.input.isEmpty() })
    }

    private fun updateTransactionsErc20(ethereumTransactions: List<EthereumTransaction>) {
        storage.saveTransactions(ethereumTransactions)

//        val contractTransactions = HashMap<String, MutableList<EthereumTransaction>>()
//
//        ethereumTransactions.forEach { transaction ->
//            val address = transaction.contractAddress
//            if (contractTransactions[address] == null) {
//                contractTransactions[address] = mutableListOf()
//            }
//            contractTransactions[address]?.add(transaction)
//        }
//
//        contractTransactions.forEach { (contractAddress, transactions) ->
//            if (erc20Contracts[contractAddress] != null) {
//                listener?.onUpdateErc20Transactions(transactions, contractAddress)
//            }
//        }
    }

    class Erc20Contract(var address: ByteArray, var syncState: EthereumKit.SyncState)

    sealed class ApiException : Exception() {
        object ContractNotRegistered : ApiException()
        object InternalException : ApiException()
    }

    companion object {
        fun getInstance(storage: IApiStorage,
                        transactionSigner: TransactionSigner,
                        transactionBuilder: TransactionBuilder,
                        address: ByteArray): ApiBlockchain {

            val apiProvider = NewApiProvider()

            return ApiBlockchain(storage, apiProvider, transactionSigner, transactionBuilder, address)
        }
    }

}
