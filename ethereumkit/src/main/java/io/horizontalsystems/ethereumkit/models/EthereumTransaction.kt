package io.horizontalsystems.ethereumkit.models

import android.arch.persistence.room.Entity
import io.horizontalsystems.ethereumkit.models.etherscan.EtherscanTransaction
import org.web3j.crypto.Keys

@Entity(primaryKeys = ["hash","contractAddress"])
class EthereumTransaction() {

    constructor(etherscanTx: EtherscanTransaction) : this() {
        hash =  etherscanTx.hash
        nonce = etherscanTx.nonce.toIntOrNull() ?: 0
        input = etherscanTx.input
        from = formatInEip55(etherscanTx.from)
        to = formatInEip55(etherscanTx.to)
        contractAddress = formatInEip55(etherscanTx.contractAddress)
        blockNumber = etherscanTx.blockNumber.toLongOrNull()
        blockHash = etherscanTx.blockHash
        value = etherscanTx.value
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
    var value: String = ""
    var gasLimit: Int = 0
    var gasPriceInWei: Long = 0
    var timeStamp: Long = 0
    var contractAddress: String = ""
    var blockHash: String = ""
    var blockNumber: Long? = null
    var confirmations: Long = 0
    var gasUsed: String = ""
    var cumulativeGasUsed: String = ""
    var iserror: String = ""
    var transactionIndex: String = ""
    var txReceiptStatus: String = ""

    private fun formatInEip55(textString: String) =
            if (textString.isEmpty()) "" else Keys.toChecksumAddress(textString)

}
