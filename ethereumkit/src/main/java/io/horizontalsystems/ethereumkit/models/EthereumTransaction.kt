package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.ethereumkit.core.storage.DatabaseConverters
import io.horizontalsystems.ethereumkit.models.etherscan.EtherscanTransaction
import org.web3j.crypto.Keys
import java.math.BigDecimal

@Entity(primaryKeys = ["hash","contractAddress"])
@TypeConverters(DatabaseConverters::class)
class EthereumTransaction() {

    constructor(etherscanTx: EtherscanTransaction, decimal: Int?) : this() {
        hash =  etherscanTx.hash
        nonce = etherscanTx.nonce.toIntOrNull() ?: 0
        input = etherscanTx.input
        from = etherscanTx.from
        contractAddress = if (etherscanTx.contractAddress.isEmpty()) "" else Keys.toChecksumAddress(etherscanTx.contractAddress)
        blockNumber = etherscanTx.blockNumber.toLongOrNull() ?: 0
        blockHash = etherscanTx.blockHash
        to = etherscanTx.to
        decimal?.let {
            val rate = BigDecimal.valueOf(Math.pow(10.0, it.toDouble()))
            value = (etherscanTx.value.toBigDecimalOrNull())?.divide(rate) ?: BigDecimal.ZERO
        }
        gasLimit = etherscanTx.gas.toIntOrNull() ?: 0
        gasPriceInWei = etherscanTx.gasPrice.toLongOrNull() ?: 0L
        timeStamp = etherscanTx.timeStamp.toLongOrNull() ?: 0
        transactionIndex = etherscanTx.transactionIndex
        iserror = etherscanTx.isError ?: ""
        txReceiptStatus = etherscanTx.txreceipt_status ?: ""
        cumulativeGasUsed = etherscanTx.cumulativeGasUsed
        gasUsed = etherscanTx.gasUsed
        confirmations = etherscanTx.confirmations.toLongOrNull() ?: 0
    }

    var hash: String = ""
    var nonce: Int = 0
    var input: String = ""
    var from: String = ""
    var to: String = ""
    var value: BigDecimal = BigDecimal.ZERO
    var gasLimit: Int = 0
    var gasPriceInWei: Long = 0
    var timeStamp: Long = 0
    var contractAddress: String = ""
    var blockHash: String = ""
    var blockNumber: Long = 0
    var confirmations: Long = 0
    var gasUsed: String = ""
    var cumulativeGasUsed: String = ""
    var iserror: String = ""
    var transactionIndex: String = ""
    var txReceiptStatus: String = ""

}
