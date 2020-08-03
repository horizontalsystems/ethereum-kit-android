package io.horizontalsystems.ethereumkit.sample.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.sample.Erc20Token
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

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

    override val syncState: EthereumKit.SyncState
        get() = convertToEthereumKitSyncState(erc20Kit.syncState)

    override val transactionsSyncState: EthereumKit.SyncState
        get() = convertToEthereumKitSyncState(erc20Kit.transactionsSyncState)

    override val balance: BigDecimal
        get() = erc20Kit.balance?.toBigDecimal()?.movePointLeft(decimals) ?: BigDecimal.ZERO

    override val receiveAddress: Address
        get() = ethereumKit.receiveAddress

    override val lastBlockHeightFlowable: Flowable<Unit>
        get() = ethereumKit.lastBlockHeightFlowable.map { Unit }

    override val syncStateFlowable: Flowable<Unit>
        get() = erc20Kit.syncStateFlowable.map { Unit }

    override val transactionsSyncStateFlowable: Flowable<Unit>
        get() = erc20Kit.transactionsSyncStateFlowable.map { Unit }

    override val balanceFlowable: Flowable<Unit>
        get() = erc20Kit.balanceFlowable.map { Unit }

    override val transactionsFlowable: Flowable<Unit>
        get() = erc20Kit.transactionsFlowable.map { Unit }

    override fun refresh() {
        erc20Kit.refresh()
    }

    override fun estimatedGasLimit(toAddress: Address?, value: BigDecimal, gasPrice: Long?): Single<Long> {
        return erc20Kit.estimateGas(toAddress, contractAddress, value.movePointRight(decimals).toBigInteger(), gasPrice)
    }

    override fun send(address: Address, amount: BigDecimal, gasPrice: Long, gasLimit: Long): Single<Unit> {
        return erc20Kit.send(address, amount.movePointRight(decimals).toBigInteger(), gasPrice, gasLimit).map { Unit }
    }

    override fun transactions(from: Pair<ByteArray, Int>?, limit: Int?): Single<List<TransactionRecord>> {
        return erc20Kit.transactions(from?.let { TransactionKey(from.first, from.second) }, limit)
                .map { transactions ->
                    transactions.map { transactionRecord(it) }
                }
    }

    fun allowance(spenderAddress: Address): Single<BigDecimal> {
        return erc20Kit.allowance(spenderAddress).map { allowance -> allowance.toBigDecimal().movePointLeft(decimals) }
    }

    fun estimateApprove(spenderAddress: Address, amount: BigDecimal, gasPrice: Long): Single<Long> {
        return erc20Kit.estimateApprove(spenderAddress, amount.movePointRight(decimals).toBigInteger(), gasPrice)
    }

    fun approve(spenderAddress: Address, amount: BigDecimal, gasPrice: Long, gasLimit: Long): Single<String> {
        return erc20Kit.approve(spenderAddress, amount.movePointRight(decimals).toBigInteger(), gasPrice, gasLimit).map {
            it.transaction.hash.toHexString()
        }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, Address(transaction.from) == mineAddress)
        val to = TransactionAddress(transaction.to, Address(transaction.to) == mineAddress)

        var amount: BigDecimal

        transaction.value.toBigDecimal().let {
            amount = it.movePointLeft(decimals)
            if (from.mine) {
                amount = -amount
            }
        }

        return TransactionRecord(
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                amount = amount,
                timestamp = transaction.timestamp,
                from = from,
                to = to,
                blockHeight = transaction.blockNumber,
                isError = transaction.isError

        )
    }

    private fun convertToEthereumKitSyncState(syncState: Erc20Kit.SyncState): EthereumKit.SyncState {
        return when (syncState) {
            Erc20Kit.SyncState.Synced -> EthereumKit.SyncState.Synced()
            Erc20Kit.SyncState.Syncing -> EthereumKit.SyncState.Syncing()
            is Erc20Kit.SyncState.NotSynced -> EthereumKit.SyncState.NotSynced(syncState.error)
        }
    }

}
