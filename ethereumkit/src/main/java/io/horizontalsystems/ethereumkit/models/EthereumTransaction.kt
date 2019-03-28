package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import io.horizontalsystems.ethereumkit.api.models.etherscan.EtherscanTransaction
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import java.math.BigInteger

@Entity
class EthereumTransaction {
    @PrimaryKey
    val hash: ByteArray
    val nonce: Long
    val input: ByteArray
    val from: ByteArray
    val to: ByteArray
    val value: BigInteger
    val gasLimit: Long
    val gasPrice: Long
    val timestamp: Long

    var blockHash: ByteArray? = null
    var blockNumber: Long? = null
    var gasUsed: Long? = null
    var cumulativeGasUsed: Long? = null
    var iserror: Int? = null
    var transactionIndex: Int? = null
    var txReceiptStatus: Int? = null

    constructor(hash: ByteArray, nonce: Long, input: ByteArray, from: ByteArray, to: ByteArray, value: BigInteger, gasLimit: Long, gasPrice: Long, timestamp: Long) {
        this.hash = hash
        this.nonce = nonce
        this.input = input
        this.from = from
        this.to = to
        this.value = value
        this.gasLimit = gasLimit
        this.gasPrice = gasPrice
        this.timestamp = timestamp
    }

    constructor(etherscanTx: EtherscanTransaction) {
        hash = etherscanTx.hash.hexStringToByteArray()
        nonce = etherscanTx.nonce.toLongOrNull() ?: 0
        input = etherscanTx.input.hexStringToByteArray()
        from = etherscanTx.from.hexStringToByteArray()
        to = etherscanTx.to.hexStringToByteArray()
        value = BigInteger(etherscanTx.value)
        gasLimit = etherscanTx.gas.toLongOrNull() ?: 0
        gasPrice = etherscanTx.gasPrice.toLongOrNull() ?: 0L
        timestamp = etherscanTx.timestamp.toLongOrNull() ?: 0

        blockHash = etherscanTx.blockHash.hexStringToByteArray()
        blockNumber = etherscanTx.blockNumber.toLongOrNull()
        gasUsed = etherscanTx.gasUsed.toLongOrNull()
        cumulativeGasUsed = etherscanTx.cumulativeGasUsed.toLongOrNull()
        iserror = etherscanTx.isError?.toIntOrNull()
        transactionIndex = etherscanTx.transactionIndex.toIntOrNull()
        txReceiptStatus = etherscanTx.txreceipt_status?.toIntOrNull()
    }
}
