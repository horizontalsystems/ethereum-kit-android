package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.ethereumkit.core.storage.DatabaseConverters
import io.horizontalsystems.ethereumkit.models.etherscan.EtherscanTransaction
import org.web3j.crypto.Keys
import java.math.BigDecimal

@Entity(primaryKeys = ["hash","contractAddress"])
@TypeConverters(DatabaseConverters::class)
class TransactionRoom(
        val hash: String,
        val nonce: Int,
        val input: String,
        val from: String,
        val to: String,
        val value: BigDecimal,
        val gasLimit: Int,
        val gasPrice: BigDecimal,
        val timeStamp: Long
) {

    constructor(etherscanTx: EtherscanTransaction) : this(
            etherscanTx.hash,
            etherscanTx.nonce.toIntOrNull() ?: 0,
            etherscanTx.input,
            etherscanTx.from,
            etherscanTx.to,
            etherscanTx.value.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            etherscanTx.gas.toIntOrNull() ?: 0,
            etherscanTx.gasPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            etherscanTx.timeStamp.toLongOrNull() ?: 0
    ) {
        contractAddress = if (etherscanTx.contractAddress.isEmpty()) "" else Keys.toChecksumAddress(etherscanTx.contractAddress)
        blockNumber = etherscanTx.blockNumber.toLongOrNull() ?: 0
        blockHash = etherscanTx.blockHash

        transactionIndex = etherscanTx.transactionIndex
        iserror = etherscanTx.isError ?: ""
        txReceiptStatus = etherscanTx.txreceipt_status ?: ""
        cumulativeGasUsed = etherscanTx.cumulativeGasUsed
        gasUsed = etherscanTx.gasUsed
        confirmations = etherscanTx.confirmations.toLongOrNull() ?: 0
    }

    var contractAddress: String = ""

    var blockHash: String? = null
    var blockNumber: Long? = null
    var confirmations: Long? = null
    var gasUsed: String? = null
    var cumulativeGasUsed: String? = null
    var iserror: String? = null
    var transactionIndex: String? = null
    var txReceiptStatus: String? = null

}
