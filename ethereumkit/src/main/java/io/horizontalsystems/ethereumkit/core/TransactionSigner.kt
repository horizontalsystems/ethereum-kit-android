package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.network.INetwork
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import java.math.BigInteger

class TransactionSigner(
        private val privateKey: BigInteger,
        private val networkId: Int
) {

    fun signature(rawTransaction: RawTransaction): Signature {
        val signatureData = sign(rawTransaction)

        return signature(signatureData)
    }

    private fun signature(signatureData: ByteArray): Signature {
        return Signature(v = (signatureData[64] + if (networkId == 0) 27 else (35 + 2 * networkId)).toByte(),
                r = signatureData.copyOfRange(0, 32),
                s = signatureData.copyOfRange(32, 64))
    }

    private fun sign(rawTransaction: RawTransaction): ByteArray {
        val encodedTransaction = RLP.encodeList(
                RLP.encodeLong(rawTransaction.nonce),
                RLP.encodeLong(rawTransaction.gasPrice),
                RLP.encodeLong(rawTransaction.gasLimit),
                RLP.encodeElement(rawTransaction.to.raw),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeElement(rawTransaction.data),
                RLP.encodeByte(networkId.toByte()),
                RLP.encodeElement(ByteArray(0)),
                RLP.encodeElement(ByteArray(0)))

        val rawTransactionHash = CryptoUtils.sha3(encodedTransaction)

        return CryptoUtils.ellipticSign(rawTransactionHash, privateKey)
    }

}
