package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.decorations.TransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.oneinchkit.decorations.OneInchUnknownDecoration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override val lastBlockHeightFlow: Flow<Unit>
        get() = ethereumKit.lastBlockHeightFlow.map { }

    override val syncStateFlow: Flow<Unit>
        get() = ethereumKit.syncStateFlow.map { }

    override val transactionsSyncStateFlow: Flow<Unit>
        get() = ethereumKit.transactionsSyncStateFlow.map { }

    override val balanceFlow: Flow<Unit>
        get() = ethereumKit.accountStateFlow.map { }

    override val transactionsFlow: Flow<Unit>
        get() = ethereumKit.allTransactionsFlow.map { }


    override fun start() {
        ethereumKit.start()
    }

    override fun stop() {
        ethereumKit.stop()
    }

    override fun refresh() {
        ethereumKit.refresh()
    }

    override suspend fun estimatedGasLimit(
        toAddress: Address,
        value: BigDecimal,
        gasPrice: GasPrice
    ): Long {
        return ethereumKit.estimateGas(
            toAddress,
            value.movePointRight(decimal).toBigInteger(),
            gasPrice
        )
    }

    override suspend fun send(
        address: Address,
        amount: BigDecimal,
        gasPrice: GasPrice,
        gasLimit: Long
    ): FullTransaction {
        throw Exception("Subclass must override")
    }

    override suspend fun transactions(fromHash: ByteArray?, limit: Int?): List<TransactionRecord> {
        return ethereumKit.getFullTransactionsAsync(listOf(), fromHash, limit)
            .map { transactionRecord(it) }
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

