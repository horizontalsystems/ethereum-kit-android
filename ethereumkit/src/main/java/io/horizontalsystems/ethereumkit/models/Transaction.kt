package io.horizontalsystems.ethereumkit.models

import io.horizontalsystems.ethereumkit.models.etherscan.EtherscanTransaction
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import org.web3j.crypto.Keys

open class Transaction : RealmObject {

    @PrimaryKey
    var compoundKey: String = ""

    var hash: String = ""
    var timeStamp: Long = 0

    var from: String = ""
    var to: String = ""
    var contractAddress: String = ""

    var value: String = ""
    var gas: Int = 0
    var gasPrice: String = ""

    var blockNumber: Long = 0
    var blockHash: String = ""

    var nonce: Int = 0
    var transactionIndex: String = ""
    var isError: String = ""
    var txReceiptStatus: String = ""
    var input: String = "0x"
    var cumulativeGasUsed: String = ""
    var gasUsed: String = ""
    var confirmations: Int = 0

    constructor()

    constructor(etherscanTx: EtherscanTransaction) {
        this.hash = etherscanTx.hash
        this.timeStamp = etherscanTx.timeStamp.toLongOrNull() ?: 0

        this.from = etherscanTx.from
        this.to = etherscanTx.to
        this.contractAddress = if (etherscanTx.contractAddress.isEmpty()) ""
        else Keys.toChecksumAddress(etherscanTx.contractAddress)

        this.value = etherscanTx.value
        this.gas = etherscanTx.gas.toIntOrNull() ?: 0
        this.gasPrice = etherscanTx.gasPrice

        this.blockNumber = etherscanTx.blockNumber.toLongOrNull() ?: 0
        this.blockHash = etherscanTx.blockHash

        this.nonce = etherscanTx.nonce.toIntOrNull() ?: 0
        this.transactionIndex = etherscanTx.transactionIndex
        this.isError = etherscanTx.isError ?: ""
        this.txReceiptStatus = etherscanTx.txreceipt_status ?: ""
        this.input = etherscanTx.input
        this.cumulativeGasUsed = etherscanTx.cumulativeGasUsed
        this.gasUsed = etherscanTx.gasUsed
        this.confirmations = etherscanTx.confirmations.toIntOrNull() ?: 0

        // Composite Primary Key from two fields
        compoundKey = hash + contractAddress
    }

}
