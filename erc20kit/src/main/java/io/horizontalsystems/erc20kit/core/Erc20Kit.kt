package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState.*
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.math.BigInteger

class Erc20Kit(
        private val contractAddress: Address,
        private val ethereumKit: EthereumKit,
        private val transactionManager: ITransactionManager,
        private val balanceManager: IBalanceManager,
        private val allowanceManager: AllowanceManager,
        private val state: KitState = KitState()
) : ITransactionManagerListener, IBalanceManagerListener {

    private val disposables = CompositeDisposable()

    sealed class SyncState {
        object Synced : SyncState()
        class NotSynced(val error: Throwable) : SyncState()
        object Syncing : SyncState()
    }

    init {
        onSyncStateUpdate(ethereumKit.syncState)
        state.balance = balanceManager.balance

        ethereumKit.syncStateFlowable
                .subscribe {
                    onSyncStateUpdate(it)
                }.let {
                    disposables.add(it)
                }

        ethereumKit.lastBlockBloomFilterFlowable
                .subscribe {
                    onUpdateLastBlockBloomFilter(it)
                }.let {
                    disposables.add(it)
                }
    }

    private fun onSyncStateUpdate(syncState: EthereumKit.SyncState) {
        when (syncState) {
            is EthereumKit.SyncState.NotSynced -> state.syncState = NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> state.syncState = Syncing
            is EthereumKit.SyncState.Synced -> {
                state.syncState = Syncing
                balanceManager.sync()
                transactionManager.immediateSync()
            }
        }
    }

    private fun onUpdateLastBlockBloomFilter(bloomFilter: BloomFilter) {
        if (bloomFilter.mayContainContractAddress(contractAddress)) {
            balanceManager.sync()
        }
    }

    val syncState: SyncState
        get() = state.syncState

    val transactionsSyncState: SyncState
        get() = state.transactionsSyncState

    val balance: BigInteger?
        get() = state.balance

    fun refresh() {
    }

    fun estimateGas(toAddress: Address?, contractAddress: Address, value: BigInteger, gasPrice: Long?): Single<Long> {
        // without address - provide default gas limit
        if (toAddress == null) {
            return Single.just(ethereumKit.defaultGasLimit)
        }

        val transactionInput = transactionManager.getTransactionInput(toAddress, value)
        return ethereumKit.estimateGas(contractAddress, null, gasPrice, transactionInput)
    }

    fun allowance(spenderAddress: Address, defaultBlockParameter: DefaultBlockParameter = DefaultBlockParameter.Latest): Single<BigInteger> {
        return allowanceManager.allowance(spenderAddress, defaultBlockParameter)
    }

    fun approve(spenderAddress: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        return allowanceManager.approve(spenderAddress, amount, gasPrice, gasLimit)
                .doOnSuccess { tx ->
                    state.transactionsSubject.onNext(listOf(tx))
                }
    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return allowanceManager.approveTransactionData(spenderAddress, amount)
    }

    fun send(to: Address, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        return transactionManager.send(to, value, gasPrice, gasLimit)
                .doOnSuccess { tx ->
                    state.transactionsSubject.onNext(listOf(tx))
                }
    }

    fun transactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return transactionManager.getTransactions(fromTransaction, limit)
    }

    val syncStateFlowable: Flowable<SyncState>
        get() = state.syncStateSubject.toFlowable(BackpressureStrategy.LATEST)

    val transactionsSyncStateFlowable: Flowable<SyncState>
        get() = state.transactionsSyncStateSubject.toFlowable(BackpressureStrategy.LATEST)

    val balanceFlowable: Flowable<BigInteger>
        get() = state.balanceSubject.toFlowable(BackpressureStrategy.LATEST)

    val transactionsFlowable: Flowable<List<Transaction>>
        get() = state.transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun stop() {
        disposables.clear()
    }

    // ITransactionManagerListener

    override fun onSyncStarted() {
       state.transactionsSyncState = Syncing
    }

    override fun onSyncSuccess(transactions: List<Transaction>) {
        state.transactionsSyncState = Synced

        if (transactions.isNotEmpty())
            state.transactionsSubject.onNext(transactions)
    }

    override fun onSyncTransactionsError(error: Throwable) {
        state.transactionsSyncState = NotSynced(error)
    }

    // IBalanceManagerListener

    override fun onSyncBalanceSuccess(balance: BigInteger) {
        if (state.balance == balance) {
            if (state.syncState == Synced) {
                transactionManager.delayedSync(false)
            }
        } else {
            transactionManager.delayedSync(true)
        }

        state.balance = balance
        state.syncState = Synced
    }

    override fun onSyncBalanceError(error: Throwable) {
        state.syncState = NotSynced(error)
    }

    companion object {

        fun getInstance(context: Context,
                        ethereumKit: EthereumKit,
                        contractAddress: Address): Erc20Kit {

            val address = ethereumKit.receiveAddress

            val erc20KitDatabase = Erc20DatabaseManager.getErc20Database(context, ethereumKit.networkType, ethereumKit.walletId, contractAddress)
            val roomStorage = Erc20Storage(erc20KitDatabase)
            val transactionStorage: ITransactionStorage = roomStorage
            val balanceStorage: ITokenBalanceStorage = roomStorage

            val dataProvider: IDataProvider = DataProvider(ethereumKit)
            val transactionsProvider = EtherscanTransactionsProvider(EtherscanService(ethereumKit.networkType, ethereumKit.etherscanKey)) // TransactionsProvider(dataProvider)
            val transactionBuilder: ITransactionBuilder = TransactionBuilder()
            val transactionManager: ITransactionManager = TransactionManager(contractAddress, address, transactionStorage, transactionsProvider, dataProvider, transactionBuilder)
            val balanceManager: IBalanceManager = BalanceManager(contractAddress, address, balanceStorage, dataProvider)
            val allowanceManager = AllowanceManager(ethereumKit, contractAddress, address, transactionStorage)

            val erc20Kit = Erc20Kit(contractAddress, ethereumKit, transactionManager, balanceManager, allowanceManager)

            transactionManager.listener = erc20Kit
            balanceManager.listener = erc20Kit

            return erc20Kit
        }

        fun clear(context: Context, networkType: EthereumKit.NetworkType, walletId: String) {
            Erc20DatabaseManager.clear(context, networkType, walletId)
        }
    }

}
