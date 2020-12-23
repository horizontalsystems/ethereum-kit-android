package io.horizontalsystems.ethereumkit.core.refactoring

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single
import java.math.BigInteger


class TransactionManager(
        private val address: Address,
        private val transactionSyncManager: TransactionSyncManager,
        private val storage: IStorage
) {

    val etherTransactionsFlowable = transactionSyncManager.transactionsFlowable
            .map { transactions ->
                transactions.filter { isEtherTransferred(it) }
            }

    fun getEtherTransactions(fromHash: ByteArray? = null, limit: Int? = null): Single<List<FullTransaction>> {
        return storage.getEtherTransactions(address, fromHash, limit)
    }

    fun handle(transaction: Transaction) {
        TODO("not implemented")
    }

    private fun isEtherTransferred(fullTransaction: FullTransaction): Boolean =
            fullTransaction.transaction.from == address && fullTransaction.transaction.value > BigInteger.ZERO ||
                    fullTransaction.transaction.to == address ||
                    fullTransaction.internalTransactions.any { it.to == address }



}
