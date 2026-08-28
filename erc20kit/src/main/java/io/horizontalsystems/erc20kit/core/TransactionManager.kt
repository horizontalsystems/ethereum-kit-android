package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.erc20kit.contract.TransferMethod
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.horizontalsystems.ethereumkit.core.bufferedSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

class TransactionManager(
        private val contractAddress: Address,
        private val ethereumKit: EthereumKit
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val transactionsSubject = bufferedSharedFlow<List<FullTransaction>>()
    private val tags: List<List<String>> = listOf(listOf(contractAddress.hex))

    val transactionsFlow: Flow<List<FullTransaction>> = transactionsSubject

    init {
        ethereumKit.getFullTransactionsFlow(tags)
                .onEach {
                    processTransactions(it)
                }
                .launchIn(scope)
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
    }

    fun buildTransferTransactionData(to: Address, value: BigInteger): TransactionData {
        return TransactionData(to = contractAddress, value = BigInteger.ZERO, TransferMethod(to, value).encodedABI())
    }

    suspend fun getTransactionsAsync(fromHash: ByteArray?, limit: Int?): List<FullTransaction> {
        return ethereumKit.getFullTransactionsAsync(tags, fromHash, limit)
    }

    fun getPendingTransactions(): List<FullTransaction> {
        return ethereumKit.getPendingFullTransactions(tags)
    }

    private fun processTransactions(erc20Transactions: List<FullTransaction>) {
        if (erc20Transactions.isNotEmpty()) {
            transactionsSubject.tryEmit(erc20Transactions)
        }
    }

}
