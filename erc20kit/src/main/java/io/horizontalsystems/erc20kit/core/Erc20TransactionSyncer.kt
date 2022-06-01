package io.horizontalsystems.erc20kit.core

import io.horizontalsystems.ethereumkit.core.IEip20Storage
import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.Eip20Event
import io.horizontalsystems.ethereumkit.models.ProviderTokenTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single

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

    override fun getTransactionsSingle(): Single<List<Transaction>> {
        val lastTransactionBlockNumber = storage.getLastEvent()?.blockNumber ?: 0

        return transactionProvider.getTokenTransactions(lastTransactionBlockNumber + 1)
            .doOnSuccess { providerTokenTransactions -> handle(providerTokenTransactions) }
            .map { providerTokenTransactions ->
                providerTokenTransactions.map { transaction ->
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
            }
    }

}
