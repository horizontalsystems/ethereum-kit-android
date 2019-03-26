package io.horizontalsystems.ethereumkit.spv.core

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.hdwalletkit.ECKey
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import java.math.BigInteger

class TransactionSigner(private val chainId: Int, private val privateKey: BigInteger) {

    fun sign(rawTransaction: RawTransaction): Signature {
        val encodedTransaction = encode(rawTransaction, Signature(chainId.toByte(), ByteArray(0), ByteArray(0)))
        val rawTransactionHash = CryptoUtils.sha3(encodedTransaction)

        val pubKey = ECKey.pubKeyFromPrivKey(privateKey, false)
        val ecKeyPair = ECKeyPair(privateKey, BigInteger(1, pubKey.slice(1 until pubKey.size).toByteArray()))

        val signatureData = Sign.signMessage(rawTransactionHash, ecKeyPair, false)

        val signature = TransactionEncoder.createEip155SignatureData(signatureData, chainId.toByte())

        return Signature(signature.v, signature.r, signature.s)
    }

    fun hash(rawTransaction: RawTransaction, signature: Signature): ByteArray {
        return CryptoUtils.sha3(encode(rawTransaction, signature))
    }

    private fun encode(rawTransaction: RawTransaction, signature: Signature): ByteArray {
        return RLP.encodeList(
                RLP.encodeBigInteger(rawTransaction.nonce),
                RLP.encodeBigInteger(rawTransaction.gasPrice),
                RLP.encodeBigInteger(rawTransaction.gasLimit),
                RLP.encodeElement(rawTransaction.to.substring(2).hexStringToByteArray()),
                RLP.encodeBigInteger(rawTransaction.value),
                RLP.encodeString(rawTransaction.data),
                RLP.encodeByte(signature.v),
                RLP.encodeElement(signature.r),
                RLP.encodeElement(signature.s))
    }

}
