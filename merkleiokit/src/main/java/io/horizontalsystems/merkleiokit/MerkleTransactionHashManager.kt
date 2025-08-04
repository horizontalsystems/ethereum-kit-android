package io.horizontalsystems.merkleiokit

class MerkleTransactionHashManager(private val dao: MerkleTransactionDao) {

    fun hashes() = dao.hashes()

    fun save(hash: MerkleTransactionHash) = dao.save(hash)

    fun handle(txHashes: List<ByteArray>) = dao.delete(txHashes)
}

