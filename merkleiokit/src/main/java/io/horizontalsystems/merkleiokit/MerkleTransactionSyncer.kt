package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.api.jsonrpc.models.RpcTransaction
import io.horizontalsystems.ethereumkit.core.ITransactionSyncer
import io.horizontalsystems.ethereumkit.models.Transaction
import io.reactivex.Single

class MerkleTransactionSyncer(
    private val manager: MerkleTransactionHashManager,
    private val blockchain: MerkleRpcBlockchain,
) : ITransactionSyncer {

    private fun convert(tx: RpcTransaction) = Transaction(
        hash = tx.hash,
        timestamp = 0,
        isFailed = false,
        blockNumber = tx.blockNumber,
        transactionIndex = tx.transactionIndex,
        from = tx.from,
        to = tx.to,
        value = tx.value,
        input = tx.input,
        nonce = tx.nonce,
        gasPrice = tx.gasPrice,
        maxFeePerGas = tx.maxFeePerGas,
        maxPriorityFeePerGas = tx.maxPriorityFeePerGas,
        gasLimit = tx.gasLimit,
        gasUsed = 0,
        replacedWith = null
    )


    override fun getTransactionsSingle(): Single<Pair<List<Transaction>, Boolean>> {
        val hashes = manager.hashes()
        if (hashes.isEmpty()) {
            return Single.just(Pair(listOf(), false))
        }

        val singles = hashes.map { tx ->
            blockchain.transaction(tx.hash).map { convert(it) }
        }

        val transactionsSingle = Single.zip(singles) {
            it.filterIsInstance<Transaction>()
        }

        return transactionsSingle
            .doOnSuccess {
                manager.handle(it)
            }
            .map {
                Pair(it, false)
            }
    }

}
