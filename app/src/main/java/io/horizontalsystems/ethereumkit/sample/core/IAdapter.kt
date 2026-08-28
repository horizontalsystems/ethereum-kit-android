package io.horizontalsystems.ethereumkit.sample.core

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.GasPrice
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

interface IAdapter {

    val name: String
    val coin: String

    val lastBlockHeight: Long?
    val syncState: EthereumKit.SyncState
    val transactionsSyncState: EthereumKit.SyncState
    val balance: BigDecimal

    val receiveAddress: Address

    val lastBlockHeightFlow: Flow<Unit>
    val syncStateFlow: Flow<Unit>
    val transactionsSyncStateFlow: Flow<Unit>
    val balanceFlow: Flow<Unit>
    val transactionsFlow: Flow<Unit>

    fun start()
    fun stop()
    fun refresh()
    suspend fun send(address: Address, amount: BigDecimal, gasPrice: GasPrice, gasLimit: Long): FullTransaction
    suspend fun transactions(fromHash: ByteArray? = null, limit: Int? = null): List<TransactionRecord>

    suspend fun estimatedGasLimit(toAddress: Address, value: BigDecimal, gasPrice: GasPrice): Long
}
