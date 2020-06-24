package io.horizontalsystems.ethereumkit.spv.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.ethereumkit.core.toHexString
import io.horizontalsystems.ethereumkit.spv.core.toLong
import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.ethereumkit.spv.rlp.RLPList
import java.math.BigInteger

@Entity
class BlockHeader {

    companion object {
        val EMPTY_TRIE_HASH = CryptoUtils.sha3(RLP.encodeElement(ByteArray(0)))
    }

    @PrimaryKey
    val height: Long
    val hashHex: ByteArray
    var totalDifficulty: BigInteger = BigInteger.ZERO // Scalar value corresponding to the sum of difficulty values of all previous blocks
    val parentHash: ByteArray         // 256-bit Keccak-256 hash of parent block
    val unclesHash: ByteArray         // 256-bit Keccak-256 hash of uncles portion of this block
    val coinbase: ByteArray           // 160-bit address for fees collected from successful mining
    val stateRoot: ByteArray          // 256-bit state trie root hash
    val transactionsRoot: ByteArray   // 256-bit transactions trie root hash
    val receiptsRoot: ByteArray       // 256-bit receipts trie root hash
    val logsBloom: ByteArray          /* The Bloom filter composed from indexable information
                                  * (logger address and log topics) contained in each log entry
                                  * from the receipt of each transaction in the transactions list */
    val difficulty: ByteArray         /* A scalar value corresponding to the difficulty level of this block.
                               * This can be calculated from the previous block’s difficulty level
                               * and the timestamp */

    val gasLimit: ByteArray         // A scalar value equal to the current limit of gas expenditure per block
    val gasUsed: Long             // A scalar value equal to the total gas used in transactions in this block
    val timestamp: Long           // A scalar value equal to the reasonable output of Unix's time() at this block's inception
    val extraData: ByteArray          /* An arbitrary byte array containing data relevant to this block.
                               * With the exception of the genesis block, this must be 32 bytes or fewer */
    val mixHash: ByteArray            /* A 256-bit hash which proves that together with nonce a sufficient amount
                               * of computation has been carried out on this block */
    val nonce: ByteArray              /* A 64-bit hash which proves that a sufficient amount
                                  * of computation has been carried out on this block */

    constructor(
            hashHex: ByteArray,
            totalDifficulty: BigInteger, // Scalar value corresponding to the sum of difficulty values of all previous blocks
            parentHash: ByteArray,         // 256-bit Keccak-256 hash of parent block
            unclesHash: ByteArray,         // 256-bit Keccak-256 hash of uncles portion of this block
            coinbase: ByteArray,           // 160-bit address for fees collected from successful mining
            stateRoot: ByteArray,          // 256-bit state trie root hash
            transactionsRoot: ByteArray,   // 256-bit transactions trie root hash
            receiptsRoot: ByteArray,       // 256-bit receipts trie root hash
            logsBloom: ByteArray,          /* The Bloom filter composed from indexable information
                                  * (logger address and log topics) contained in each log entry
                                  * from the receipt of each transaction in the transactions list */
            difficulty: ByteArray,         /* A scalar value corresponding to the difficulty level of this block.
                               * This can be calculated from the previous block’s difficulty level
                               * and the timestamp */
            height: Long,
            gasLimit: ByteArray,         // A scalar value equal to the current limit of gas expenditure per block
            gasUsed: Long,             // A scalar value equal to the total gas used in transactions in this block
            timestamp: Long,           // A scalar value equal to the reasonable output of Unix's time() at this block's inception
            extraData: ByteArray,          /* An arbitrary byte array containing data relevant to this block.
                               * With the exception of the genesis block, this must be 32 bytes or fewer */
            mixHash: ByteArray,            /* A 256-bit hash which proves that together with nonce a sufficient amount
                            * of computation has been carried out on this block */
            nonce: ByteArray              /* A 64-bit hash which proves that a sufficient amount
                                  * of computation has been carried out on this block */
    ) {
        this.hashHex = hashHex
        this.totalDifficulty = totalDifficulty
        this.parentHash = parentHash
        this.unclesHash = unclesHash
        this.coinbase = coinbase
        this.stateRoot = stateRoot
        this.transactionsRoot = transactionsRoot
        this.receiptsRoot = receiptsRoot
        this.logsBloom = logsBloom
        this.difficulty = difficulty
        this.height = height
        this.gasLimit = gasLimit
        this.gasUsed = gasUsed
        this.timestamp = timestamp
        this.extraData = extraData
        this.mixHash = mixHash
        this.nonce = nonce
    }

    constructor(rlpHeader: RLPList) {

        this.hashHex = CryptoUtils.sha3(rlpHeader.rlpData ?: ByteArray(0))
        this.parentHash = rlpHeader[0].rlpData ?: byteArrayOf()
        this.unclesHash = rlpHeader[1].rlpData ?: byteArrayOf()
        this.coinbase = rlpHeader[2].rlpData ?: byteArrayOf()
        this.stateRoot = rlpHeader[3].rlpData ?: byteArrayOf()

        val txsRoot = rlpHeader[4].rlpData
        this.transactionsRoot = if (txsRoot == null || txsRoot.isEmpty()) EMPTY_TRIE_HASH else txsRoot

        val rcptsRoot = rlpHeader[5].rlpData
        this.receiptsRoot = if (rcptsRoot == null || rcptsRoot.isEmpty()) EMPTY_TRIE_HASH else rcptsRoot

        this.logsBloom = rlpHeader[6].rlpData ?: byteArrayOf()
        this.difficulty = rlpHeader[7].rlpData ?: byteArrayOf()
        this.height = rlpHeader[8].rlpData.toLong()
        this.gasLimit = rlpHeader[9].rlpData ?: byteArrayOf()
        this.gasUsed = rlpHeader[10].rlpData.toLong()
        this.timestamp = rlpHeader[11].rlpData.toLong()
        this.extraData = rlpHeader[12].rlpData ?: byteArrayOf()
        this.mixHash = rlpHeader[13].rlpData ?: byteArrayOf()
        this.nonce = rlpHeader[14].rlpData ?: byteArrayOf()
    }

    override fun toString(): String {
        return "(hash: ${hashHex.toHexString()}; height: $height; parentHash: ${parentHash.toHexString()})"
    }
}
