package io.horizontalsystems.ethereumkit.light.crypto

import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger

interface ICrypto {
    fun randomECKey(): ECKey
    fun randomBytes(length: Int): ByteArray

    fun ecdhAgree(myKey: ECKey, remotePublicKeyPoint: ECPoint): ByteArray

    fun ellipticSign(messageToSign: ByteArray, key: ECKey): ByteArray

    fun eciesDecrypt(privateKey: BigInteger, message: ECIESEncryptedMessage): ByteArray
    fun eciesEncrypt(remotePublicKey: ECPoint, message: ByteArray): ECIESEncryptedMessage

    fun sha3(data: ByteArray): ByteArray

    fun encryptAES(key: ByteArray, data: ByteArray): ByteArray
}