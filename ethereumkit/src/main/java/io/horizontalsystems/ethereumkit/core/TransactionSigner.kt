package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class TransactionSigner(private val network: INetwork, private val privateKey: BigInteger) {

    private val EMPTY_BYTE_ARRAY = ByteArray(0)

    fun sign(rawTransaction: RawTransaction, nonce: Long): Signature {
        val encodedTransaction = RLP.encodeList(
                RLP.encodeLong(nonce),
                RLP.encodeLong(rawTransaction.gasPrice),
                RLP.encodeLong(rawTransaction.gasLimit),
                RLP.encodeElement(rawTransaction.to),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeElement(rawTransaction.data),
                RLP.encodeByte(network.id.toByte()),
                RLP.encodeElement(EMPTY_BYTE_ARRAY),
                RLP.encodeElement(EMPTY_BYTE_ARRAY))

        val rawTransactionHash = CryptoUtils.sha3(encodedTransaction)
        val signature = CryptoUtils.ellipticSign(rawTransactionHash, privateKey)

        return calculateVRS(signature)
    }

    private fun calculateVRS(signature: ByteArray): Signature {
        return Signature(v = (signature[64] + if (network.id == 0) 27 else (35 + 2 * network.id)).toByte(),
                r = signature.copyOfRange(0, 32),
                s = signature.copyOfRange(32, 64))
    }
}
