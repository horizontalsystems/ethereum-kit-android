package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.refactoring.Erc20TransactionSyncer
import io.horizontalsystems.erc20kit.core.refactoring.TransactionManagerNew
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.contracts.ContractMethodFactories
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.refactoring.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.BloomFilter
import io.horizontalsystems.ethereumkit.models.DefaultBlockParameter
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.network.EtherscanService
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.math.BigInteger

class Erc20Kit(
        private val contractAddress: Address,
        private val ethereumKit: EthereumKit,
        private val transactionManager: TransactionManagerNew,
        private val balanceManager: IBalanceManager,
        private val allowanceManager: AllowanceManager,
        private val state: KitState = KitState()
) : IBalanceManagerListener {

    private val disposables = CompositeDisposable()

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

    private fun onSyncStateUpdate(syncState: SyncState) {
        when (syncState) {
            is SyncState.NotSynced -> state.syncState = SyncState.NotSynced(syncState.error)
            is SyncState.Syncing -> state.syncState = SyncState.Syncing()
            is SyncState.Synced -> {
                state.syncState = SyncState.Syncing()
                balanceManager.sync()
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
        get() = ethereumKit.transactionsSyncState

    val balance: BigInteger?
        get() = state.balance

    fun start() {
        transactionManager.sync()
    }

    fun stop() {
        ethereumKit.removeTransactionSyncer(getTransactionSyncerId(contractAddress))

        disposables.clear()
    }

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

//    fun approve(spenderAddress: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
//        return allowanceManager.approve(spenderAddress, amount, gasPrice, gasLimit)
//    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return allowanceManager.approveTransactionData(spenderAddress, amount)
    }

//    fun send(to: Address, value: BigInteger, gasPrice: Long, gasLimit: Long): Single<Transaction> {
//        return transactionManager.send(to, value, gasPrice, gasLimit)
//                .doOnSuccess { tx ->
//                    state.transactionsSubject.onNext(listOf(tx))
//                }
//    }

    fun transactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<Transaction>> {
        return transactionManager.getTransactionsAsync(fromTransaction, limit)
    }

    fun pendingTransactions(): List<Transaction> {
        return transactionManager.getPendingTransactions()
    }

    val syncStateFlowable: Flowable<SyncState>
        get() = state.syncStateSubject.toFlowable(BackpressureStrategy.LATEST)

    val transactionsSyncStateFlowable: Flowable<SyncState>
        get() = ethereumKit.transactionsSyncStateFlowable

    val balanceFlowable: Flowable<BigInteger>
        get() = state.balanceSubject.toFlowable(BackpressureStrategy.LATEST)

    val transactionsFlowable: Flowable<List<Transaction>>
        get() = transactionManager.transactionsAsync

    // IBalanceManagerListener

    override fun onSyncBalanceSuccess(balance: BigInteger) {
//        if (state.balance == balance) {
//            if (state.syncState is SyncState.Synced) {
//                transactionManager.delayedSync(false)
//            }
//        } else {
//            transactionManager.delayedSync(true)
//        }

        state.balance = balance
        state.syncState = SyncState.Synced()
    }

    override fun onSyncBalanceError(error: Throwable) {
        state.syncState = SyncState.NotSynced(error)
    }

    companion object {

        fun getInstance(
                context: Context,
                ethereumKit: EthereumKit,
                contractAddress: Address
        ): Erc20Kit {

            val address = ethereumKit.receiveAddress

            val erc20KitDatabase = Erc20DatabaseManager.getErc20Database(context, ethereumKit.networkType, ethereumKit.walletId, contractAddress)
            val roomStorage = Erc20Storage(erc20KitDatabase)
            val transactionStorage: ITransactionStorage = roomStorage
            val balanceStorage: ITokenBalanceStorage = roomStorage

            val dataProvider: IDataProvider = DataProvider(ethereumKit)
            val transactionManager = TransactionManagerNew(contractAddress, ethereumKit, ContractMethodFactories, transactionStorage)
            val balanceManager: IBalanceManager = BalanceManager(contractAddress, address, balanceStorage, dataProvider)
            val allowanceManager = AllowanceManager(ethereumKit, contractAddress, address)

            val erc20Kit = Erc20Kit(contractAddress, ethereumKit, transactionManager, balanceManager, allowanceManager)

            balanceManager.listener = erc20Kit

            val syncerId = getTransactionSyncerId(contractAddress)
            val transactionsProvider = EtherscanTransactionsProvider(EtherscanService(ethereumKit.networkType, ethereumKit.etherscanKey))
            val erc20TransactionSyncer: ITransactionSyncer = Erc20TransactionSyncer(syncerId, address, contractAddress, transactionsProvider)

            ethereumKit.addTransactionSyncer(erc20TransactionSyncer)

            return erc20Kit
        }

        private fun getTransactionSyncerId(contractAddress: Address): String {
            return "Erc20TransactionSyncer-${contractAddress.hex}"
        }

        fun clear(context: Context, networkType: EthereumKit.NetworkType, walletId: String) {
            Erc20DatabaseManager.clear(context, networkType, walletId)
        }
    }

}
