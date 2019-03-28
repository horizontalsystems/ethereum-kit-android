package io.horizontalsystems.ethereumkit.core

import io.horizontalsystems.ethereumkit.spv.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.spv.models.RawTransaction
import io.horizontalsystems.ethereumkit.spv.models.Signature
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import io.horizontalsystems.hdwalletkit.ECKey
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
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

        val pubKey = ECKey.pubKeyFromPrivKey(privateKey, false)
        val ecKeyPair = ECKeyPair(privateKey, BigInteger(1, pubKey.slice(1 until pubKey.size).toByteArray()))

        val signatureData = Sign.signMessage(rawTransactionHash, ecKeyPair, false)
        val signature = TransactionEncoder.createEip155SignatureData(signatureData, network.id.toByte())

        return Signature(signature.v, signature.r, signature.s)
    }
}
