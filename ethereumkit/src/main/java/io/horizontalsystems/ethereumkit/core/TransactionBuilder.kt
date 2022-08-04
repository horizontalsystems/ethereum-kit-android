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
                timestamp = System.currentTimeMillis() / 1000,
                nonce = rawTransaction.nonce,
                input = rawTransaction.data,
                from = address,
                to = rawTransaction.to,
                value = rawTransaction.value,
                gasPrice = rawTransaction.gasPrice.max,
                maxFeePerGas = maxFeePerGas,
                maxPriorityFeePerGas = maxPriorityFeePerGas,
                gasLimit = rawTransaction.gasLimit,
                isFailed = false,
        )
    }

    fun encode(rawTransaction: RawTransaction, signature: Signature): ByteArray =
            encode(rawTransaction, signature, chainId)

    companion object {

        fun encode(rawTransaction: RawTransaction, signature: Signature?, chainId: Int = 1): ByteArray {
            val signatureArray = signature?.let {
                arrayOf(
                        RLP.encodeInt(it.v),
                        RLP.encodeBigInteger(it.r.toBigInteger()),
                        RLP.encodeBigInteger(it.s.toBigInteger())
                )
            } ?: arrayOf()

            return when (rawTransaction.gasPrice) {
                is GasPrice.Eip1559 -> {
                    val elements = arrayOf(
                            RLP.encodeInt(chainId),
                            RLP.encodeLong(rawTransaction.nonce),
                            RLP.encodeLong(rawTransaction.gasPrice.maxPriorityFeePerGas),
                            RLP.encodeLong(rawTransaction.gasPrice.maxFeePerGas),
                            RLP.encodeLong(rawTransaction.gasLimit),
                            RLP.encodeElement(rawTransaction.to.raw),
                            RLP.encodeBigInteger(rawTransaction.value),
                            RLP.encodeElement(rawTransaction.data),
                            RLP.encode(arrayOf<Any>())
                    ) + signatureArray

                    val encodedTransaction = RLP.encodeList(*elements)
                    "0x02".hexStringToByteArray() + encodedTransaction
                }
                is GasPrice.Legacy -> {
                    val elements = arrayOf(
                            RLP.encodeLong(rawTransaction.nonce),
                            RLP.encodeLong(rawTransaction.gasPrice.legacyGasPrice),
                            RLP.encodeLong(rawTransaction.gasLimit),
                            RLP.encodeElement(rawTransaction.to.raw),
                            RLP.encodeBigInteger(rawTransaction.value),
                            RLP.encodeElement(rawTransaction.data)
                    ) + signatureArray

                    RLP.encodeList(*elements)
                }
            }
        }

    }
}
