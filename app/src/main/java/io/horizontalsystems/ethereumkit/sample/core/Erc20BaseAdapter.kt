package io.horizontalsystems.ethereumkit.sample.core

import android.content.Context
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.EthereumKit.SyncState
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.sample.modules.main.Erc20Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.math.BigInteger

open class Erc20BaseAdapter(
    context: Context,
    token: Erc20Token,
    private val ethereumKit: EthereumKit
) : IAdapter {

    private val contractAddress: Address = token.contractAddress
    protected val decimals: Int = token.decimals
    protected val erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress)

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

    override val lastBlockHeightFlow: Flow<Unit>
        get() = ethereumKit.lastBlockHeightFlow.map { }

    override val syncStateFlow: Flow<Unit>
        get() = erc20Kit.syncStateFlow.map { }

    override val transactionsSyncStateFlow: Flow<Unit>
        get() = erc20Kit.transactionsSyncStateFlow.map { }

    override val balanceFlow: Flow<Unit>
        get() = erc20Kit.balanceFlow.map { }

    override val transactionsFlow: Flow<Unit>
        get() = erc20Kit.transactionsFlow.map { }

    override fun start() {
        erc20Kit.start()
    }

    override fun stop() {
        erc20Kit.stop()
    }

    override fun refresh() {
        erc20Kit.refresh()
    }

    override suspend fun estimatedGasLimit(
        toAddress: Address,
        value: BigDecimal,
        gasPrice: GasPrice
    ): Long {
        val valueBigInteger = value.movePointRight(decimals).toBigInteger()
        val transactionData = erc20Kit.buildTransferTransactionData(toAddress, valueBigInteger)
        return ethereumKit.estimateGas(transactionData, gasPrice)
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
        return erc20Kit.getTransactionsAsync(fromHash, limit)
            .map { transactionRecord(it) }
    }

    fun approveTransactionData(spenderAddress: Address, amount: BigInteger): TransactionData {
        return erc20Kit.buildApproveTransactionData(spenderAddress, amount)
    }

    private fun transactionRecord(fullTransaction: FullTransaction): TransactionRecord {
        val transaction = fullTransaction.transaction
        var amount: BigDecimal = 0.toBigDecimal()

        transaction.value?.toBigDecimal()?.let {
            amount = it.movePointLeft(decimals)
        }

        return TransactionRecord(
            transactionHash = transaction.hash.toHexString(),
            timestamp = transaction.timestamp,
            isError = fullTransaction.transaction.isFailed,
            from = transaction.from,
            to = transaction.to,
            amount = amount,
            blockHeight = fullTransaction.transaction.blockNumber,
            transactionIndex = fullTransaction.transaction.transactionIndex ?: 0,
            decoration = fullTransaction.decoration.describe()
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
