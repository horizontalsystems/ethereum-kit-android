package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class EthereumAdapter(ethereumKit: EthereumKit) : BaseAdapter(ethereumKit, 18) {

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncState

    override val balanceBigInteger: BigInteger?
        get() = ethereumKit.balance

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

    val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    val syncStateFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    val balanceFlowable: Flowable<Unit>
        get() = ethereumKit.balanceFlowable.map { Unit }

    val transactionsFlowable: Flowable<Unit>
        get() = ethereumKit.transactionsFlowable.map { Unit }

}
