package io.horizontalsystems.ethereumkit.core.signer

import io.horizontalsystems.ethereumkit.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.crypto.EIP712Encoder
import io.horizontalsystems.ethereumkit.crypto.TypedData
import java.math.BigInteger

class EthSigner(
    private val privateKey: BigInteger,
    private val cryptoUtils: CryptoUtils,
    private val eip712Encoder: EIP712Encoder
) {

    fun signByteArray(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n" + message.size
        val hashedMessage = cryptoUtils.sha3(prefix.toByteArray() + message)
        return sign(hashedMessage)
    }

    fun signByteArrayLegacy(message: ByteArray): ByteArray {
        return sign(message)
    }

    fun signTypedData(rawJsonMessage: String): ByteArray {
        val encodedMessage = eip712Encoder.encodeTypedDataHash(rawJsonMessage)
        return sign(encodedMessage)
    }

    fun parseTypedData(rawJsonMessage: String): TypedData? {
        return eip712Encoder.parseTypedData(rawJsonMessage)
    }

    private fun sign(message: ByteArray): ByteArray = cryptoUtils.ellipticSign(message, privateKey)

}
