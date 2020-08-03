package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
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

    override val transactionsSyncState: EthereumKit.SyncState
        get() = ethereumKit.transactionsSyncState

    override val balance: BigDecimal
        get() = ethereumKit.balance?.toBigDecimal()?.movePointLeft(decimal) ?: BigDecimal.ZERO

    override val receiveAddress: Address
        get() = ethereumKit.receiveAddress

    override val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    override val syncStateFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    override val transactionsSyncStateFlowable: Flowable<Unit>
        get() = ethereumKit.transactionsSyncStateFlowable.map { Unit }

    override val balanceFlowable: Flowable<Unit>
        get() = ethereumKit.syncStateFlowable.map { Unit }

    override val transactionsFlowable: Flowable<Unit>
        get() = ethereumKit.transactionsFlowable.map { Unit }

    override fun refresh() {
        ethereumKit.refresh()
    }

    override fun estimatedGasLimit(toAddress: Address?, value: BigDecimal, gasPrice: Long?): Single<Long> {
        return ethereumKit.estimateGas(toAddress, value.movePointRight(decimal).toBigInteger(), gasPrice)
    }

    override fun send(address: Address, amount: BigDecimal, gasPrice: Long, gasLimit: Long): Single<Unit> {
        return ethereumKit.send(address, amount.movePointRight(decimal).toBigInteger(), gasPrice, gasLimit).map { Unit }
    }

    override fun transactions(from: Pair<ByteArray, Int>?, limit: Int?): Single<List<TransactionRecord>> {
        return ethereumKit.transactions(from?.first, limit).map { transactions ->
            transactions.map { transactionRecord(it) }
        }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val fromAddressHex = transaction.from
        val from = TransactionAddress(fromAddressHex, Address(fromAddressHex) == mineAddress)

        val toAddressHex = transaction.to
        val to = TransactionAddress(toAddressHex, Address(toAddressHex) == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimal)
            if (from.mine) {
                amount = -amount
            }
        }

        transaction.internalTransactions.forEach { internalTransaction ->
            var internalAmount = internalTransaction.value.toBigDecimalOrNull()?.movePointLeft(decimal)
                    ?: BigDecimal.ZERO
            val outgoing = Address(internalTransaction.from) == receiveAddress
            internalAmount = if (outgoing) internalAmount.negate() else internalAmount
            amount += internalAmount
        }

        return TransactionRecord(
                transactionHash = transaction.hash,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = 0,
                blockHeight = transaction.blockNumber,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to,
                isError = transaction.isError?.let { it != 0 } ?: false
        )
    }
}

