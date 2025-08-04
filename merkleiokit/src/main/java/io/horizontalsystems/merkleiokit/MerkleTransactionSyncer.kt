package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.core.IExtraDecorator
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.TransactionManager
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single
import kotlin.jvm.optionals.getOrNull

class MerkleTransactionSyncer(
    private val manager: MerkleTransactionHashManager,
    private val blockchain: MerkleRpcBlockchain,
    private val transactionManager: TransactionManager,
) : ITransactionSyncer, IExtraDecorator {

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
            val completedTxHashes = mutableListOf<ByteArray>()
            val failedTxHashes = mutableListOf<ByteArray>()
            val failedTxs = mutableListOf<Transaction>()

            rpcTransactions.forEach { (hash, rpcTransaction) ->
                if (rpcTransaction == null) {
                    failedTxHashes.add(hash)

                    transactionManager.getFullTransactions(listOf(hash)).firstOrNull()?.let {
                        failedTxs.add(it.transaction.copy(isFailed = true))
                    }
                } else if (rpcTransaction.blockNumber != null) {
                    completedTxHashes.add(hash)
                }
            }

            manager.handle(completedTxHashes + failedTxHashes)

            Pair(failedTxs, false)
        }
    }

    override fun extra(hash: ByteArray): Map<String, Any> {
        val merkleTransactionHash = manager.hash(hash)

        return if (merkleTransactionHash != null) {
            mapOf(MerkleTransactionAdapter.protectedKey to true)
        } else {
            mapOf()
        }
    }
}
