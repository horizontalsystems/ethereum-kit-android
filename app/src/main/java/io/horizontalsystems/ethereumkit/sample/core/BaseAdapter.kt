package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.EthereumKit
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import java.math.BigDecimal

open class BaseAdapter(val ethereumKit: EthereumKit, val decimal: Int) : EthereumKit.Listener {

    val transactionSubject = PublishSubject.create<Unit>()
    val balanceSubject = PublishSubject.create<Unit>()
    val lastBlockHeightSubject = PublishSubject.create<Unit>()
    val syncStateUpdateSubject = PublishSubject.create<Unit>()

    open val syncState: EthereumKit.SyncState = EthereumKit.SyncState.NotSynced

    fun transactionRecord(transaction: EthereumTransaction): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, transaction.from == mineAddress)

        val to = TransactionAddress(transaction.to, transaction.to == mineAddress)

        var amount: BigDecimal = BigDecimal.valueOf(0.0)

        transaction.value.toBigDecimalOrNull()?.let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.hash,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timeStamp,
                from = from,
                to = to
        )
    }

    val balance: BigDecimal
        get() {
            balanceString?.toBigDecimalOrNull()?.let {
                val converted = it.movePointLeft(decimal)
                return converted.stripTrailingZeros()
            }

            return BigDecimal.ZERO
        }

    open val balanceString: String?
        get() {
            return null
        }

    open fun sendSingle(address: String, amount: BigDecimal): Single<Unit> {
        val poweredDecimal = amount.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0)

        return sendSingle(address, noScaleDecimal.toPlainString())
    }

    open fun sendSingle(address: String, amount: String): Single<Unit> {
        return Single.just(Unit)
    }

    open fun transactionsSingle(hashFrom: String? = null, limit: Int? = null): Single<List<TransactionRecord>> {
        return transactionsObservable(hashFrom, limit)
                .map { list -> list.map { transactionRecord(it) } }
    }

    open fun transactionsObservable(hashFrom: String? = null, limit: Int? = null): Single<List<EthereumTransaction>> {
        return Single.just(listOf())
    }

    override fun onTransactionsUpdate(transactions: List<EthereumTransaction>) {
        transactionSubject.onNext(Unit)
    }

    override fun onBalanceUpdate() {
        balanceSubject.onNext(Unit)
    }

    override fun onLastBlockHeightUpdate() {
        lastBlockHeightSubject.onNext(Unit)
    }

    override fun onSyncStateUpdate() {
        syncStateUpdateSubject.onNext(Unit)
    }
}
