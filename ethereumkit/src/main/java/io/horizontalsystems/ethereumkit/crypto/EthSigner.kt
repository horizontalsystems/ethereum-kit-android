package io.horizontalsystems.ethereumkit.crypto

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

    fun signTypedData(rawJsonMessage: String): ByteArray {
        val encodedMessage = eip712Encoder.encodeTypedDataHash(rawJsonMessage)
        return sign(encodedMessage)
    }

    private fun sign(message: ByteArray): ByteArray = cryptoUtils.ellipticSign(message, privateKey)

}
