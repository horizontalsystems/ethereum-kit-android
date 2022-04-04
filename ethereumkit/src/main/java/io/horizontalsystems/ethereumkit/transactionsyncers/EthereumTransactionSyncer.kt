package io.horizontalsystems.ethereumkit.transactionsyncers

import io.horizontalsystems.ethereumkit.core.ITransactionProvider
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.Transaction

class EthereumTransactionSyncer(
        private val transactionProvider: ITransactionProvider
): ITransactionSyncer {

    override fun getTransactionsSingle(lastTransactionBlockNumber: Long) =
        transactionProvider.getTransactions(lastTransactionBlockNumber + 1).map { providerTransactions ->
            providerTransactions.map { transaction ->
                val isFailed = when {
                    transaction.txReceiptStatus != null -> {
                        transaction.txReceiptStatus != 1
                    }
                    transaction.isError != null -> {
                        transaction.isError != 0
                    }
                    transaction.gasUsed != null -> {
                        transaction.gasUsed == transaction.gasLimit
                    }
                    else -> {
                        false
                    }
                }

                Transaction(
                    hash = transaction.hash,
                    timestamp = transaction.timestamp,
                    isFailed = isFailed,
                    blockNumber = transaction.blockNumber,
                    transactionIndex = transaction.transactionIndex,
                    from = transaction.from,
                    to = transaction.to,
                    value = transaction.value,
                    input = transaction.input,
                    nonce = transaction.nonce,
                    gasPrice = transaction.gasPrice,
                    gasUsed = transaction.gasUsed
                )
            }
        }

}
