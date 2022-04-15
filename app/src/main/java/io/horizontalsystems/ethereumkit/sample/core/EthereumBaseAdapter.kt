package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.oneinchkit.decorations.OneInchUnknownDecoration
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
        return ethereumKit.getFullTransactionsAsync(listOf(), fromHash, limit)
            .map { transactions ->
                transactions.map { transactionRecord(it) }
            }
    }

    private fun transactionRecord(fullTransaction: FullTransaction): TransactionRecord {
        val transaction = fullTransaction.transaction
        val mineAddress = ethereumKit.receiveAddress

        var amount: BigDecimal = 0.toBigDecimal()

        transaction.value?.toBigDecimal()?.let {
            amount = it.movePointLeft(decimal)
        }

        return TransactionRecord(
            transactionHash = transaction.hash.toHexString(),
            timestamp = transaction.timestamp,
            isError = fullTransaction.transaction.isFailed,
            from = transaction.from,
            to = transaction.to,
            amount = amount,
            blockHeight = transaction.blockNumber,
            transactionIndex = transaction.transactionIndex ?: 0,
            decoration = fullTransaction.decoration.describe()
        )
    }
}

fun TransactionDecoration.describe(): String =
    when (this) {
        is OneInchUnknownDecoration -> {
            val _out = this.tokenAmountOut?.let { "${it.value} (${it.token.toString()})" } ?: "n/a"
            val _in = this.tokenAmountIn?.let { "${it.value} (${it.token.toString()})" } ?: "n/a"

            "OneInchUnknownDecoration($_out <-> $_in)"
        }

        else -> this.toString()
    }

