package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class EthereumAdapter(private val ethereumKit: EthereumKit) : IAdapter {

    private val decimal = 18

    override val name: String
        get() = "Ether"

    override val coin: String
        get() = "ETH"

    override val lastBlockHeight: Long?
        get() = ethereumKit.lastBlockHeight

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncState

    override val balance: BigDecimal
        get() = ethereumKit.balance?.toBigDecimal()?.movePointLeft(decimal) ?: BigDecimal.ZERO

    override val receiveAddress: String
        get() = ethereumKit.receiveAddress

    override val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    override val syncStateFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    override val balanceFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    override val transactionsFlowable: Flowable<Unit>
        get() = ethereumKit.transactionsFlowable.map { Unit }

    override fun validateAddress(address: String) {
        ethereumKit.validateAddress(address)
    }

    override fun estimatedGasLimit(toAddress: String, value: BigDecimal): Single<Int> {
        return Single.just(ethereumKit.gasLimit)
    }

    override fun send(address: String, amount: BigDecimal, gasLimit: Int): Single<Unit> {
        val poweredDecimal = amount.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0)

        return ethereumKit.send(address, noScaleDecimal.toPlainString(), 5_000_000_000, gasLimit).map { Unit }
    }

    override fun transactions(from: Pair<String, Int>?, limit: Int?): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(from?.first, limit).map { transactions ->
            transactions.map { transactionRecord(it) }
        }
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
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = 0,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to
        )
    }
}

