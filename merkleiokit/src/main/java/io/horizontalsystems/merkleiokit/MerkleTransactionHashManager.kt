package io.horizontalsystems.merkleiokit

import io.horizontalsystems.ethereumkit.models.Transaction

class MerkleTransactionHashManager(private val dao: MerkleTransactionDao) {

    private val _hasMerkleTransactions = mutableMapOf<Int, Boolean>()

    fun hasMerkleTransactions(chainId: Int): Boolean {
        _hasMerkleTransactions[chainId]?.let {
            return it
        }

        val hasTxs = dao.hasMerkleTransactions(chainId)

        _hasMerkleTransactions[chainId] = hasTxs

        return hasTxs
    }

    fun hashes(chainId: Int): List<MerkleTransactionHash> {
        return dao.hashes(chainId)
    }

    fun save(hash: MerkleTransactionHash) {
        dao.save(hash)
        _hasMerkleTransactions[hash.chainId] = true
    }

    fun handle(transactions: List<Transaction>, chainId: Int) {
        val toRemove = transactions.mapNotNull { tx ->
            tx.blockNumber?.let {
                MerkleTransactionHash(tx.hash, chainId)
            }
        }

        if (toRemove.isNotEmpty()) {
            dao.delete(toRemove)

            _hasMerkleTransactions[chainId] = dao.hasMerkleTransactions(chainId)
        }
    }
}

