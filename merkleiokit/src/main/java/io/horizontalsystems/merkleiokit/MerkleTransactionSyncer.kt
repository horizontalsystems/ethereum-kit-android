package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.core.IExtraDecorator
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.core.TransactionManager
import io.horizontalsystems.ethereumkit.models.Transaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.jvm.optionals.getOrNull

class MerkleTransactionSyncer(
    private val manager: MerkleTransactionHashManager,
    private val blockchain: MerkleRpcBlockchain,
    private val transactionManager: TransactionManager,
) : ITransactionSyncer, IExtraDecorator {

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getTransactions(): Pair<List<Transaction>, Boolean> {
        val hashes = manager.hashes()
        if (hashes.isEmpty()) return Pair(listOf(), false)

        // Fetch concurrently; drop the ones that failed, keep the successful ones
        val rpcTransactions = coroutineScope {
            hashes.map { tx ->
                async {
                    try {
                        Pair(tx.hash, blockchain.transaction(tx.hash).getOrNull())
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

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

        return Pair(failedTxs, false)
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
