package io.horizontalsystems.ethereumkit.light.net.connection

import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

class EncryptionHandshakeTest {

    @Before
    fun setUp() {
    }

    @Test
    fun createAuthMessage() {
        val myKey = CryptoUtils.ecKeyFromPrivate(BigInteger("38208918395832628331087730025239389699013035341486183519748173810236817397977"))

        val remotePublicKey = ByteArray(65)
        val nodeId = "1baf02c18c08ab0d009ccc9b51168be6a8776509ff229a6ca08507b53579cb99e0df1709bd1bcf64aed348f9a31298842cf12c1764c8de9d28abb921a548ad8c".hexStringToByteArray()
        System.arraycopy(nodeId, 0, remotePublicKey, 1, nodeId.size)
        remotePublicKey[0] = 0x04 // uncompressed

        val encryptionHandshake = EncryptionHandshake(myKey, CryptoUtils.CURVE.curve.decodePoint(remotePublicKey))

        encryptionHandshake.createAuthMessage()


    }
}