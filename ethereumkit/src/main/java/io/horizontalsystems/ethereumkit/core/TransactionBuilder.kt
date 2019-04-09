package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.models.EthereumTransaction
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class TransactionBuilder {

    fun rawTransaction(gasPrice: Long, gasLimit: Long, to: ByteArray, value: BigInteger): RawTransaction {
        return RawTransaction(gasPrice, gasLimit, to, value)
    }

    fun rawErc20Transaction(contractAddress: ByteArray, gasPrice: Long, gasLimit: Long, to: ByteArray, value: BigInteger): RawTransaction {
        val data = ByteArray(0)

        // todo: create data for erc20 transfer

        return RawTransaction(gasPrice, gasLimit, contractAddress, value, data)
    }

    fun transaction(rawTransaction: RawTransaction, nonce: Long, signature: Signature, address: ByteArray): EthereumTransaction {
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
                RLP.encodeElement(rawTransaction.to),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeElement(rawTransaction.data),
                RLP.encodeByte(signature.v),
                RLP.encodeElement(signature.r),
                RLP.encodeElement(signature.s))
    }
}
