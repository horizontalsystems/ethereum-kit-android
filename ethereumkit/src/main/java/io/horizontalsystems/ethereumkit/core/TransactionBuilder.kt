package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP

class TransactionBuilder(private val address: Address) {

    fun transaction(rawTransaction: RawTransaction, signature: Signature): Transaction {
        val transactionHash = CryptoUtils.sha3(encode(rawTransaction, signature))

        return Transaction(
                hash = transactionHash,
                nonce = rawTransaction.nonce,
                input = rawTransaction.data,
                from = address,
                to = rawTransaction.to,
                value = rawTransaction.value,
                gasLimit = rawTransaction.gasLimit,
                gasPrice = rawTransaction.gasPrice,
                timestamp = System.currentTimeMillis() / 1000
        )
    }

    fun encode(rawTransaction: RawTransaction, signature: Signature): ByteArray {
        return RLP.encodeList(
                RLP.encodeLong(rawTransaction.nonce),
                RLP.encodeLong(rawTransaction.gasPrice),
                RLP.encodeLong(rawTransaction.gasLimit),
                RLP.encodeElement(rawTransaction.to.raw),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeElement(rawTransaction.data),
                RLP.encodeByte(signature.v),
                RLP.encodeBigInteger(signature.r.toBigInteger()),
                RLP.encodeBigInteger(signature.s.toBigInteger()))
    }
}
