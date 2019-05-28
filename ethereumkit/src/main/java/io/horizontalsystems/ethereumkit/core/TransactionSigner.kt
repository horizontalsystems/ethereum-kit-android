package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class TransactionSigner(private val network: INetwork,
                        private val privateKey: BigInteger) {

    fun signature(rawTransaction: RawTransaction, nonce: Long): Signature {
        val signatureData = sign(rawTransaction, nonce)

        return signature(signatureData)
    }

    fun signature(signatureData: ByteArray): Signature {
        return Signature(v = (signatureData[64] + if (network.id == 0) 27 else (35 + 2 * network.id)).toByte(),
                r = signatureData.copyOfRange(0, 32),
                s = signatureData.copyOfRange(32, 64))
    }

    fun sign(rawTransaction: RawTransaction, nonce: Long): ByteArray {
        val encodedTransaction = RLP.encodeList(
                RLP.encodeLong(nonce),
                RLP.encodeLong(rawTransaction.gasPrice),
                RLP.encodeLong(rawTransaction.gasLimit),
                RLP.encodeElement(rawTransaction.to),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeElement(rawTransaction.data),
                RLP.encodeByte(network.id.toByte()),
                RLP.encodeElement(ByteArray(0)),
                RLP.encodeElement(ByteArray(0)))

        val rawTransactionHash = CryptoUtils.sha3(encodedTransaction)

        return CryptoUtils.ellipticSign(rawTransactionHash, privateKey)
    }

}
