package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

open class EthereumBaseAdapter(private val ethereumKit: EthereumKit) : IAdapter {

    private val decimal = 18

    override val name: String
        get() = "Ether"

    override val coin: String
        get() = "ETH"

    override val lastBlockHeight: Long?
        get() = ethereumKit.lastBlockHeight

    override val syncState: EthereumKit.SyncState
        get() = ethereumKit.syncState

    override val transactionsSyncState: EthereumKit.SyncState
        get() = ethereumKit.transactionsSyncState

    override val balance: BigDecimal
        get() = ethereumKit.accountState?.balance?.toBigDecimal()?.movePointLeft(decimal)
            ?: BigDecimal.ZERO

    override val receiveAddress: Address
        get() = ethereumKit.receiveAddress

    override val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { }

    override val syncStateFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { }

    override val transactionsSyncStateFlowable: Flowable<Unit>
        get() = ethereumKit.transactionsSyncStateFlowable.map { }

    override val balanceFlowable: Flowable<Unit>
        get() = ethereumKit.accountStateFlowable.map { }

    override val transactionsFlowable: Flowable<Unit>
        get() = ethereumKit.allTransactionsFlowable.map { }


    override fun start() {
        ethereumKit.start()
    }

    override fun stop() {
        ethereumKit.stop()
    }

    override fun refresh() {
        ethereumKit.refresh()
    }

    override fun estimatedGasLimit(
        toAddress: Address,
        value: BigDecimal,
        gasPrice: GasPrice
    ): Single<Long> {
        return ethereumKit.estimateGas(
            toAddress,
            value.movePointRight(decimal).toBigInteger(),
            gasPrice
        )
    }

    override fun send(
        address: Address,
        amount: BigDecimal,
        gasPrice: GasPrice,
        gasLimit: Long
    ): Single<FullTransaction> {
        throw Exception("Subclass must override")
    }

    override fun transactions(fromHash: ByteArray?, limit: Int?): Single<List<TransactionRecord>> {
        return ethereumKit.getTransactionsAsync(listOf(listOf("ETH")), fromHash, limit)
            .map { transactions ->
                transactions.map { transactionRecord(it) }
            }
    }

    private fun transactionRecord(fullTransaction: FullTransaction): TransactionRecord {
        val transaction = fullTransaction.transaction
        val receipt = fullTransaction.receiptWithLogs?.receipt
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from.hex, transaction.from == mineAddress)
        val to = TransactionAddress(transaction.to!!.hex, transaction.to == mineAddress)
        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        fullTransaction.internalTransactions.forEach { internalTransaction ->
            var internalAmount = internalTransaction.value.toBigDecimal().movePointLeft(decimal)
            internalAmount =
                if (internalTransaction.from == receiveAddress) internalAmount.negate() else internalAmount
            amount += internalAmount
        }

        return TransactionRecord(
            transactionHash = transaction.hash.toHexString(),
            transactionIndex = receipt?.transactionIndex ?: 0,
            interTransactionIndex = 0,
            blockHeight = receipt?.blockNumber,
            amount = amount,
            timestamp = transaction.timestamp,
            from = from,
            to = to,
            isError = fullTransaction.isFailed(),
            mainDecoration = fullTransaction.mainDecoration,
            eventsDecorations = fullTransaction.eventDecorations
        )
    }
}
