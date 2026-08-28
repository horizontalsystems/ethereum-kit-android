package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.core.IEip20Storage
import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.Eip20Event
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import kotlinx.coroutines.CancellationException

class Erc20TransactionSyncer(
        private val transactionProvider: ITransactionProvider,
        private val storage: IEip20Storage
) : ITransactionSyncer {

    private fun handle(transactions: List<ProviderTokenTransaction>) {
        if (transactions.isEmpty()) return

        val events = transactions.map { tx ->
            Eip20Event(tx.hash, tx.blockNumber, tx.contractAddress, tx.from, tx.to, tx.value, tx.tokenName, tx.tokenSymbol, tx.tokenDecimal)
        }

        storage.save(events)
    }

    override suspend fun getTransactions(): Pair<List<Transaction>, Boolean> {
        val lastTransactionBlockNumber = storage.getLastEvent()?.blockNumber ?: 0
        val initial: Boolean = lastTransactionBlockNumber == 0L

        return try {
            val providerTokenTransactions = transactionProvider.getTokenTransactions(lastTransactionBlockNumber + 1)
            handle(providerTokenTransactions)

            val array = providerTokenTransactions.map { transaction ->
                Transaction(
                        hash = transaction.hash,
                        timestamp = transaction.timestamp,
                        isFailed = false,
                        blockNumber = transaction.blockNumber,
                        transactionIndex = transaction.transactionIndex,
                        nonce = transaction.nonce,
                        gasPrice = transaction.gasPrice,
                        gasLimit = transaction.gasLimit,
                        gasUsed = transaction.gasUsed
                )
            }
            Pair(array, initial)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Pair(listOf(), initial)
        }
    }

}
