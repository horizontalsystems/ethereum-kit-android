package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.TransactionManager
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single
import kotlin.jvm.optionals.getOrNull

class MerkleTransactionSyncer(
    private val manager: MerkleTransactionHashManager,
    private val blockchain: MerkleRpcBlockchain,
    private val transactionManager: TransactionManager,
) : ITransactionSyncer {

    @OptIn(ExperimentalStdlibApi::class)
    override fun getTransactionsSingle(): Single<Pair<List<Transaction>, Boolean>> {
        val hashes = manager.hashes()
        if (hashes.isEmpty()) return Single.just(Pair(listOf(), false))

        val singles = hashes.map { tx ->
            blockchain.transaction(tx.hash)
                .map { Pair(tx.hash, it.getOrNull()) }
                .map { Result.success(it) }
                .onErrorReturn { Result.failure(it) }
        }

        val transactionsSingle = Single.merge(singles)
            .filter { it.isSuccess } // Only keep successful ones
            .map { it.getOrThrow() } // Extract the actual value
            .toList()

        return transactionsSingle.map { rpcTransactions ->
            val processedTxHashes = mutableListOf<ByteArray>()
            val failedTxs = mutableListOf<Transaction>()

            rpcTransactions.forEach { (hash, rpcTransaction) ->
                processedTxHashes.add(hash)

                if (rpcTransaction == null) {
                    transactionManager.getFullTransactions(listOf(hash)).firstOrNull()?.let {
                        failedTxs.add(it.transaction.copy(isFailed = true))
                    }
                }
            }

            manager.handleProcessed(processedTxHashes)

            Pair(failedTxs, false)
        }
    }
}
