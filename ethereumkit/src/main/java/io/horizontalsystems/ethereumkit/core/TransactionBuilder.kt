package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.core.toBigInteger
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class TransactionBuilder(private val address: Address) {

    fun rawTransaction(gasPrice: Long, gasLimit: Long, to: Address, value: BigInteger, transactionInput: ByteArray = ByteArray(0)): RawTransaction {
        return RawTransaction(gasPrice, gasLimit, to, value, transactionInput)
    }

    fun transaction(rawTransaction: RawTransaction, nonce: Long, signature: Signature): EthereumTransaction {
        val transactionHash = CryptoUtils.sha3(encode(rawTransaction, nonce, signature))

        return EthereumTransaction(
                hash = transactionHash,
                nonce = nonce,
                input = rawTransaction.data,
                from = address,
                to = rawTransaction.to,
                value = rawTransaction.value,
                gasLimit = rawTransaction.gasLimit,
                gasPrice = rawTransaction.gasPrice,
                timestamp = System.currentTimeMillis() / 1000
        )
    }

    fun encode(rawTransaction: RawTransaction, nonce: Long, signature: Signature): ByteArray {
        return RLP.encodeList(
                RLP.encodeLong(nonce),
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
