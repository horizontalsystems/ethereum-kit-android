package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.*
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.rlp.RLP

class TransactionBuilder(
        private val address: Address,
        private val chainId: Int
) {

    fun transaction(rawTransaction: RawTransaction, signature: Signature): Transaction {
        val transactionHash = CryptoUtils.sha3(encode(rawTransaction, signature))
        var maxFeePerGas: Long? = null
        var maxPriorityFeePerGas: Long? = null

        if (rawTransaction.gasPrice is GasPrice.Eip1559) {
            maxFeePerGas = rawTransaction.gasPrice.maxFeePerGas
            maxPriorityFeePerGas = rawTransaction.gasPrice.maxPriorityFeePerGas
        }

        return Transaction(
                hash = transactionHash,
                nonce = rawTransaction.nonce,
                input = rawTransaction.data,
                from = address,
                to = rawTransaction.to,
                value = rawTransaction.value,
                gasLimit = rawTransaction.gasLimit,
                gasPrice = rawTransaction.gasPrice.max,
                maxFeePerGas = maxFeePerGas,
                maxPriorityFeePerGas = maxPriorityFeePerGas,
                timestamp = System.currentTimeMillis() / 1000
        )
    }

    fun encode(rawTransaction: RawTransaction, signature: Signature): ByteArray {
        return when (rawTransaction.gasPrice) {
            is GasPrice.Eip1559 -> {
                val encodedTransaction = RLP.encodeList(
                        RLP.encodeInt(chainId),
                        RLP.encodeLong(rawTransaction.nonce),
                        RLP.encodeLong(rawTransaction.gasPrice.maxPriorityFeePerGas),
                        RLP.encodeLong(rawTransaction.gasPrice.maxFeePerGas),
                        RLP.encodeLong(rawTransaction.gasLimit),
                        RLP.encodeElement(rawTransaction.to.raw),
                        RLP.encodeBigInteger(rawTransaction.value),
                        RLP.encodeElement(rawTransaction.data),
                        RLP.encode(arrayOf<Any>()),
                        RLP.encodeByte(signature.v),
                        RLP.encodeBigInteger(signature.r.toBigInteger()),
                        RLP.encodeBigInteger(signature.s.toBigInteger())
                )
                "0x02".hexStringToByteArray() + encodedTransaction
            }
            is GasPrice.Legacy -> {
                RLP.encodeList(
                        RLP.encodeLong(rawTransaction.nonce),
                        RLP.encodeLong(rawTransaction.gasPrice.legacyGasPrice),
                        RLP.encodeLong(rawTransaction.gasLimit),
                        RLP.encodeElement(rawTransaction.to.raw),
                        RLP.encodeBigInteger(rawTransaction.value),
                        RLP.encodeElement(rawTransaction.data),
                        RLP.encodeByte(signature.v),
                        RLP.encodeBigInteger(signature.r.toBigInteger()),
                        RLP.encodeBigInteger(signature.s.toBigInteger())
                )
            }
        }
    }
}
