package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState.*
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.BigInteger

class Erc20Kit(
        private val ethereumKit: EthereumKit,
        private val transactionManager: ITransactionManager,
        private val balanceManager: IBalanceManager,
        private val gasLimit: Int = 1000000,
        private val state: KitState = KitState()
) : ITransactionManagerListener, IBalanceManagerListener {

    private val disposables = CompositeDisposable()

    sealed class SyncState {
        object Synced : SyncState()
        object NotSynced : SyncState()
        object Syncing : SyncState()
    }

    init {
        onSyncStateUpdate(ethereumKit.syncState)
        state.balance = balanceManager.balance

        ethereumKit.syncStateFlowable
                .subscribe { syncState ->
                    onSyncStateUpdate(syncState)
                }.let {
                    disposables.add(it)
                }
    }

    private fun onSyncStateUpdate(syncState: EthereumKit.SyncState) {
        when (syncState) {
            is EthereumKit.SyncState.NotSynced -> state.syncState = NotSynced
            is EthereumKit.SyncState.Syncing -> state.syncState = Syncing
            is EthereumKit.SyncState.Synced -> {
                state.syncState = Syncing
                balanceManager.sync()
            }
        }
    }

    val syncState: SyncState
        get() = state.syncState

    val balance: BigInteger?
        get() = state.balance

    fun fee(gasPrice: Long): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimit.toBigDecimal())
    }

    fun estimateGas( toAddress: String, contractAddress: String, value: BigInteger ): Single<Int> {
        val transactionInput = transactionManager.getTransactionInput(toAddress.hexStringToByteArray(), value)

        return ethereumKit.estimateGas(contractAddress, null, gasLimit,  transactionInput)
    }


    fun send(to: String, value: String, gasPrice: Long, gasLimit: Int): Single<TransactionInfo> {
        return transactionManager.send(to.hexStringToByteArray(), value.toBigInteger(), gasPrice, gasLimit)
                .map { TransactionInfo(it) }
                .doOnSuccess { txInfo ->
                    state.transactionsSubject.onNext(listOf(txInfo))
                }
    }

    fun transactions(fromTransaction: TransactionKey?, limit: Int?): Single<List<TransactionInfo>> {
        return transactionManager.getTransactions(fromTransaction, limit)
                .map { transactions ->
                    transactions.map {
                        TransactionInfo(it)
                    }
                }
    }

    val syncStateFlowable: Flowable<SyncState>
        get() = state.syncStateSubject.toFlowable(BackpressureStrategy.LATEST)

    val balanceFlowable: Flowable<BigInteger>
        get() = state.balanceSubject.toFlowable(BackpressureStrategy.LATEST)

    val transactionsFlowable: Flowable<List<TransactionInfo>>
        get() = state.transactionsSubject.toFlowable(BackpressureStrategy.BUFFER)

    fun stop() {
        disposables.clear()
    }

    // ITransactionManagerListener

    override fun onSyncSuccess(transactions: List<Transaction>) {
        state.syncState = Synced

        if (transactions.isNotEmpty())
            state.transactionsSubject.onNext(transactions.map { TransactionInfo(it) })
    }

    override fun onSyncTransactionsError() {
        state.syncState = NotSynced
    }

    // IBalanceManagerListener


    override fun onSyncBalanceSuccess(balance: BigInteger) {
        state.balance = balance
        transactionManager.sync()
    }

    override fun onSyncBalanceError() {
        state.syncState = NotSynced
    }

    companion object {

        fun getInstance(context: Context,
                        ethereumKit: EthereumKit,
                        contractAddress: String,
                        gasLimit: Int = 1_000_000): Erc20Kit {

            val contractAddressRaw = contractAddress.hexStringToByteArray()
            val address = ethereumKit.receiveAddressRaw

            val erc20KitDatabase = Erc20DatabaseManager.getErc20Database(context, ethereumKit.networkType, ethereumKit.walletId, contractAddress)
            val roomStorage = Erc20Storage(erc20KitDatabase)
            val transactionStorage: ITransactionStorage = roomStorage
            val balanceStorage: ITokenBalanceStorage = roomStorage

            val dataProvider: IDataProvider = DataProvider(ethereumKit)
            val transactionBuilder: ITransactionBuilder = TransactionBuilder()
            val transactionManager: ITransactionManager = TransactionManager(contractAddressRaw, address, transactionStorage, dataProvider, transactionBuilder)
            val balanceManager: IBalanceManager = BalanceManager(contractAddressRaw, address, balanceStorage, dataProvider)

            val erc20Kit = Erc20Kit(ethereumKit, transactionManager, balanceManager, gasLimit)

            transactionManager.listener = erc20Kit
            balanceManager.listener = erc20Kit

            return erc20Kit
        }

        fun clear(context: Context, networkType: EthereumKit.NetworkType, walletId: String) {
            Erc20DatabaseManager.clear(context, networkType, walletId)
        }
    }

}
