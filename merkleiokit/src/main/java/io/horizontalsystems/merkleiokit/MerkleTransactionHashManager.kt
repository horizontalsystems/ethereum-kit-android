package io.horizontalsystems.merkleiokit

class MerkleTransactionHashManager(private val dao: MerkleTransactionDao) {

    fun hashes() = dao.hashes()

    fun save(hash: MerkleTransactionHash) = dao.save(hash)

    fun handleProcessed(confirmedTxHashes: List<ByteArray>) = dao.delete(confirmedTxHashes)
}

