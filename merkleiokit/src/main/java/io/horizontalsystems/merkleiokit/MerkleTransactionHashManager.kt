package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.models.Transaction

class MerkleTransactionHashManager(private val dao: MerkleTransactionDao) {

    fun hashes(): List<MerkleTransactionHash> {
        return dao.hashes()
    }

    fun save(hash: MerkleTransactionHash) {
        dao.save(hash)
    }

    fun handle(transactions: List<Transaction>) {
        val toRemove = transactions.mapNotNull { tx ->
            tx.blockNumber?.let {
                MerkleTransactionHash(tx.hash)
            }
        }

        if (toRemove.isNotEmpty()) {
            dao.delete(toRemove)
        }
    }
}

