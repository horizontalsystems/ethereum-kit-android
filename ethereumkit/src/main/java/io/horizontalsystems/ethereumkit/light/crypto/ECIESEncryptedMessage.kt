package io.horizontalsystems.ethereumkit.light.crypto

import java.io.ByteArrayInputStream

class ECIESEncryptedMessage(val prefixBytes: ByteArray,
                            val ephemeralPubKey: ByteArray,
                            val initialVector: ByteArray,
                            val cipher: ByteArray,
                            val checkSum: ByteArray) {

    fun encoded(): ByteArray {
        return prefixBytes + ephemeralPubKey + initialVector + cipher + checkSum
    }

    companion object {

        fun decode(data: ByteArray): ECIESEncryptedMessage {
            val inputStream = ByteArrayInputStream(data)

            val prefixBytes = ByteArray(2)
            inputStream.read(prefixBytes)

            val ephemBytes = ByteArray(65)
            inputStream.read(ephemBytes)

            val initVector = ByteArray(16)
            inputStream.read(initVector)

            val cipherBody = ByteArray(inputStream.available() - 32)
            inputStream.read(cipherBody)

            val checkSum = ByteArray(32)
            inputStream.read(checkSum)

            return ECIESEncryptedMessage(prefixBytes, ephemBytes, initVector, cipherBody, checkSum)
        }
    }
}