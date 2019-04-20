package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionInfo
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class EthereumAdapter(ethereumKit: EthereumKit) : BaseAdapter(ethereumKit, 18), EthereumKit.Listener {

    init {
        ethereumKit.addListener(this)
    }

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncState

    override val balanceString: String?
        get() = ethereumKit.balance?.toString()

    override fun sendSingle(address: String, amount: String): Single<Unit> {
        return ethereumKit.send(address, BigInteger(amount), 5_000_000_000).map { Unit }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val fromAddressHex = transaction.from
        val from = TransactionAddress(fromAddressHex, fromAddressHex == mineAddress)

        val toAddressHex = transaction.to
        val to = TransactionAddress(toAddressHex, toAddressHex == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.hash,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to
        )
    }

    override fun transactionsSingle(hashFrom: String?, limit: Int?): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(hashFrom, limit).map { transactions ->
            transactions.map { transactionRecord(it) }
        }
    }

    override fun onTransactionsUpdate(transactions: List<TransactionInfo>) {
        transactionSubject.onNext(Unit)
    }

    override fun onClear() {
        TODO("not implemented")
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
