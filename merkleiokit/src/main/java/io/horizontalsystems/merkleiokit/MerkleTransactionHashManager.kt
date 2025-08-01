package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.models.Transaction

class MerkleTransactionHashManager(private val dao: MerkleTransactionDao) {

    fun hasMerkleTransactions(chainId: Int): Boolean {
        return dao.hasMerkleTransactions(chainId)
    }

    fun hashes(chainId: Int): List<MerkleTransactionHash> {
        return dao.hashes(chainId)
    }

    fun save(hash: MerkleTransactionHash) {
        dao.save(hash)
    }

    fun handle(transactions: List<Transaction>, chainId: Int) {
        val toRemove = transactions.mapNotNull { tx ->
            tx.blockNumber?.let {
                MerkleTransactionHash(tx.hash, chainId)
            }
        }

        if (toRemove.isNotEmpty()) {
            dao.delete(toRemove)
        }
    }
}

