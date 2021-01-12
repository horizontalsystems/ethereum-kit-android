package io.horizontalsystems.ethereumkit.sample.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.sample.Erc20Token
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class Erc20Adapter(
        context: Context,
        token: Erc20Token,
        private val ethereumKit: EthereumKit
) : IAdapter {

    private val contractAddress: Address = token.contractAddress
    private val decimals: Int = token.decimals
    private val erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress)

    override val name: String = token.name
    override val coin: String = token.code

    override val lastBlockHeight: Long?
        get() = ethereumKit.lastBlockHeight

    override val syncState: SyncState
        get() = convertToEthereumKitSyncState(erc20Kit.syncState)

    override val transactionsSyncState: SyncState
        get() = convertToEthereumKitSyncState(erc20Kit.transactionsSyncState)

    override val balance: BigDecimal
        get() = erc20Kit.balance?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO

    override val receiveAddress: Address
        get() = ethereumKit.receiveAddress

    override val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { }

    override val syncStateFlowable: Flowable<Unit>
        get() = erc20Kit.syncStateFlowable.map { }

    override val transactionsSyncStateFlowable: Flowable<Unit>
        get() = erc20Kit.transactionsSyncStateFlowable.map { }

    override val balanceFlowable: Flowable<Unit>
        get() = erc20Kit.balanceFlowable.map { }

    override val transactionsFlowable: Flowable<Unit>
        get() = erc20Kit.transactionsFlowable.map { }

    override fun start() {
        erc20Kit.start()
    }

    override fun stop() {
        erc20Kit.stop()
    }

    override fun refresh() {
        erc20Kit.refresh()
    }

    override fun estimatedGasLimit(toAddress: Address, value: BigDecimal, gasPrice: Long?): Single<Long> {
        val valueBigInteger = value.movePointRight(decimals).toBigInteger()
        val transactionData = erc20Kit.buildTransferTransactionData(toAddress, valueBigInteger)
        return ethereumKit.estimateGas(transactionData, gasPrice)
    }

    override fun send(address: Address, amount: BigDecimal, gasPrice: Long, gasLimit: Long): Single<FullTransaction> {
        val valueBigInteger = amount.movePointRight(decimals).toBigInteger()
        val transactionData = erc20Kit.buildTransferTransactionData(address, valueBigInteger)

        return ethereumKit.send(transactionData, gasPrice, gasLimit)
    }

    override fun transactions(from: Pair<ByteArray, Int>?, limit: Int?): Single<List<TransactionRecord>> {
        return erc20Kit.getTransactionsAsync(from?.let { TransactionKey(from.first, from.second) }, limit)
                .map { transactions ->
                    transactions.map { transactionRecord(it) }
                }
    }

    fun allowance(spenderAddress: Address): Single<BigDecimal> {
        return erc20Kit.getAllowanceAsync(spenderAddress).map { allowance -> allowance.toBigDecimal().movePointLeft(decimals) }
    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return erc20Kit.buildApproveTransactionData(spenderAddress, amount)
    }

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from.hex, transaction.from == mineAddress)
        val to = TransactionAddress(transaction.to.hex, transaction.to == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimals)
            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.transactionHash.toHexString(),
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to,
                blockHeight = transaction.fullTransaction.receiptWithLogs?.receipt?.blockNumber,
                isError = transaction.isError,
                type = transaction.type.value
        )
    }

    private fun convertToEthereumKitSyncState(syncState: SyncState): SyncState {
        return when (syncState) {
            is SyncState.Synced -> SyncState.Synced()
            is SyncState.Syncing -> SyncState.Syncing()
            is SyncState.NotSynced -> SyncState.NotSynced(syncState.error)
        }
    }

}
