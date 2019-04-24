package io.horizontalsystems.erc20kit.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState.*
import io.horizontalsystems.erc20kit.models.TokenBalance
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.BigInteger

class Erc20Kit(private val ethereumKit: EthereumKit,
               private val transactionManager: ITransactionManager,
               private val balanceManager: IBalanceManager,
               private val tokenHolder: ITokenHolder = TokenHolder()) : ITransactionManagerListener, IBalanceManagerListener {

    private val gasLimit: Long = 100_000
    private val disposables = CompositeDisposable()

    sealed class SyncState {
        object Synced : SyncState()
        object NotSynced : SyncState()
        object Syncing : SyncState()
    }

    init {
        ethereumKit.syncStateFlowable
                .subscribeOn(Schedulers.io())
                .subscribe { syncState ->
                    onSyncStateUpdate(syncState)
                }.let {
                    disposables.add(it)
                }

        ethereumKit.lastBlockHeightFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    onLastBlockHeightUpdate()
                }.let {
                    disposables.add(it)
                }
    }

    private fun onLastBlockHeightUpdate() {
        if (ethereumKit.syncState == EthereumKit.SyncState.Synced) {
            transactionManager.sync()
        }
    }

    private fun onSyncStateUpdate(syncState: EthereumKit.SyncState) {
        when (syncState) {
            EthereumKit.SyncState.NotSynced -> setAll(NotSynced)
            EthereumKit.SyncState.Syncing -> setAll(Syncing)
            EthereumKit.SyncState.Synced -> transactionManager.sync()
        }
    }

    private fun set(syncState: SyncState, contractAddress: ByteArray) {
        tokenHolder.set(syncState, contractAddress)
        tokenHolder.syncStateSubject(contractAddress).onNext(syncState)
    }

    private fun setAll(syncState: SyncState) {
        for (contractAddress in tokenHolder.contractAddresses) {
            set(syncState, contractAddress)
        }
    }

    fun syncState(contractAddress: String): SyncState {
        return tokenHolder.syncState(contractAddress.hexStringToByteArray())
    }

    fun balance(contractAddress: String): BigInteger {
        return tokenHolder.balance(contractAddress.hexStringToByteArray()).value
    }

    fun fee(gasPrice: Int): BigDecimal {
        return BigDecimal(gasPrice).multiply(gasLimit.toBigDecimal())
    }

    fun sendSingle(contractAddress: String, to: String, value: String, gasPrice: Long): Single<TransactionInfo> {
        return transactionManager.sendSingle(contractAddress.hexStringToByteArray(),
                to.hexStringToByteArray(),
                value.toBigInteger(10),
                gasPrice, gasLimit)
                .map { TransactionInfo(it) }
                .doOnSuccess { txInfo ->
                    tokenHolder.transactionsSubject(contractAddress.hexStringToByteArray()).onNext(listOf(txInfo))
                }
    }

    fun transactionsSingle(contractAddress: String, hashFrom: ByteArray?, indexFrom: Int?, limit: Int?): Single<List<TransactionInfo>> {
        return transactionManager.transactionsSingle(contractAddress.hexStringToByteArray(), hashFrom, indexFrom, limit)
                .map { transactions ->
                    transactions.map {
                        TransactionInfo(it)
                    }
                }
    }

    fun register(contractAddress: String, balancePosition: Int) {
        val contractAddr = contractAddress.hexStringToByteArray()
        val balance = balanceManager.balance(contractAddr)

        tokenHolder.register(contractAddr, balancePosition, balance)
    }

    fun syncStateFlowable(contractAddress: String): Flowable<SyncState> {
        return tokenHolder.syncStateSubject(contractAddress.hexStringToByteArray()).toFlowable(BackpressureStrategy.BUFFER)
    }

    fun balanceFlowable(contractAddress: String): Flowable<BigInteger> {
        return tokenHolder.balanceSubject(contractAddress.hexStringToByteArray()).toFlowable(BackpressureStrategy.BUFFER)
    }

    fun transactionsFlowable(contractAddress: String): Flowable<List<TransactionInfo>> {
        return tokenHolder.transactionsSubject(contractAddress.hexStringToByteArray()).toFlowable(BackpressureStrategy.BUFFER)
    }

    fun unregister(contractAddress: String) {
        tokenHolder.unregister(contractAddress.hexStringToByteArray())
    }

    fun clear() {
        tokenHolder.clear()
        transactionManager.clear()
        balanceManager.clear()
        disposables.clear()
    }

    // ITransactionManagerListener

    override fun onSyncSuccess(transactions: List<Transaction>) {
        val transactionsMap = transactions.groupBy { it.contractAddress }

        for ((contractAddress, transactionsList) in transactionsMap) {
            val transactionsSubject = try {
                tokenHolder.transactionsSubject(contractAddress)
            } catch (ex: NotRegisteredToken) {
                continue
            }
            transactionsSubject.onNext(transactionsList.map { TransactionInfo(it) })
        }

        for (contractAddress in tokenHolder.contractAddresses) {
            val balancePosition = try {
                tokenHolder.balancePosition(contractAddress)
            } catch (exception: NotRegisteredToken) {
                continue
            }

            val lastTransactionBlockHeight = transactionManager.lastTransactionBlockHeight(contractAddress)

            if (lastTransactionBlockHeight != null && lastTransactionBlockHeight > balanceManager.balance(contractAddress).blockHeight) {
                balanceManager.sync(lastTransactionBlockHeight, contractAddress, balancePosition)
            } else {
                set(Synced, contractAddress)
            }
        }
    }

    override fun onSyncTransactionsError() {
        setAll(NotSynced)
    }

    // IBalanceManagerListener

    override fun onBalanceUpdate(balance: TokenBalance, contractAddress: ByteArray) {
        tokenHolder.set(balance, contractAddress)
        tokenHolder.balanceSubject(contractAddress).onNext(balance.value)
    }

    override fun onSyncBalanceSuccess(contractAddress: ByteArray) {
        set(Synced, contractAddress)
    }

    override fun onSyncBalanceError(contractAddress: ByteArray) {
        set(NotSynced, contractAddress)
    }

    open class TokenError : Exception()
    class NotRegisteredToken : TokenError()


    companion object {

        fun getInstance(context: Context,
                        ethereumKit: EthereumKit): Erc20Kit {

            val address = ethereumKit.receiveAddressRaw

            val roomStorage = RoomStorage(context, "erc20_tokens_db")
            val transactionStorage: ITransactionStorage = roomStorage
            val balanceStorage: ITokenBalanceStorage = roomStorage

            val dataProvider: IDataProvider = DataProvider(ethereumKit)
            val transactionBuilder: ITransactionBuilder = TransactionBuilder()
            val transactionManager: ITransactionManager = TransactionManager(address, transactionStorage, dataProvider, transactionBuilder)
            val balanceManager: IBalanceManager = BalanceManager(address, balanceStorage, dataProvider)

            val erc20Kit = Erc20Kit(ethereumKit, transactionManager, balanceManager)

            transactionManager.listener = erc20Kit
            balanceManager.listener = erc20Kit

            return erc20Kit
        }
    }

}
