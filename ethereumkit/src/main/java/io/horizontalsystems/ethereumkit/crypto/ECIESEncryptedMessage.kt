package io.horizontalsystems.ethereumkit.crypto

import java.io.ByteArrayInputStream

class ECIESEncryptedMessage {

    val prefixBytes: ByteArray
    val ephemeralPublicKey: ByteArray
    val initialVector: ByteArray
    val cipher: ByteArray
    val checkSum: ByteArray

    constructor(prefixBytes: ByteArray,
                ephemeralPublicKey: ByteArray,
                initialVector: ByteArray,
                cipher: ByteArray,
                checkSum: ByteArray) {
        this.prefixBytes = prefixBytes
        this.ephemeralPublicKey = ephemeralPublicKey
        this.initialVector = initialVector
        this.cipher = cipher
        this.checkSum = checkSum
    }

    constructor(data: ByteArray) {
        val inputStream = ByteArrayInputStream(data)

        prefixBytes = ByteArray(2)
        inputStream.read(prefixBytes)

        ephemeralPublicKey = ByteArray(65)
        inputStream.read(ephemeralPublicKey)

        initialVector = ByteArray(16)
        inputStream.read(initialVector)

        cipher = ByteArray(inputStream.available() - 32)
        inputStream.read(cipher)

        checkSum = ByteArray(32)
        inputStream.read(checkSum)
    }

    fun encoded(): ByteArray {
        return prefixBytes + ephemeralPublicKey + initialVector + cipher + checkSum
    }
}