package io.horizontalsystems.ethereumkit.light.net.connection

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.light.crypto.CryptoUtils
import io.horizontalsystems.ethereumkit.light.crypto.ECIESEncryptedMessage
import io.horizontalsystems.ethereumkit.light.crypto.ECKey
import io.horizontalsystems.ethereumkit.light.crypto.ICrypto
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthAckMessage
import io.horizontalsystems.ethereumkit.light.net.connection.messages.AuthMessage
import io.horizontalsystems.ethereumkit.light.rlp.RLP
import io.horizontalsystems.ethereumkit.light.xor
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.math.ec.ECPoint

@RunWith(PowerMockRunner::class)
@PrepareForTest(EncryptionHandshake::class)
class EncryptionHandshakeTest {

    private lateinit var encryptionHandshake: EncryptionHandshake
    private lateinit var myKey: ECKey
    private lateinit var ephemeralKey: ECKey
    private lateinit var remoteKeyPoint: ECPoint
    private lateinit var encodedAuthECIESMessage: ByteArray

    private val crypto = Mockito.mock(ICrypto::class.java)
    private val nonce = ByteArray(32) { 0 }
    private val remoteNonce = ByteArray(32) { 1 }
    private val signature = ByteArray(32) { 5 }
    private val authECIESMessage = ECIESEncryptedMessage(ByteArray(0), ByteArray(0), ByteArray(0), ByteArray(0), ByteArray(0))

    @Before
    fun setUp() {
        myKey = CryptoUtils.randomECKey()
        ephemeralKey = CryptoUtils.randomECKey()
        remoteKeyPoint = CryptoUtils.randomECKey().publicKeyPoint
        encodedAuthECIESMessage = authECIESMessage.encoded()

        whenever(crypto.randomBytes(32)).thenReturn(nonce)
        whenever(crypto.randomKey()).thenReturn(ephemeralKey)

        encryptionHandshake = EncryptionHandshake(myKey, remoteKeyPoint, crypto)

        verify(crypto).randomKey()
        verify(crypto).randomBytes(32)
    }

    @Test
    fun createMessage() {
        val junkData = ByteArray(102) { 2 }
        val sharedSecret = ByteArray(32) { 3 }
        val authMessage = AuthMessage(signature, myKey.publicKeyPoint, nonce)

        whenever(crypto.randomBytes(ArgumentMatchers.intThat { it >= 100 })).thenReturn(junkData)
        whenever(crypto.eciesEncrypt(remoteKeyPoint, authMessage.encoded() + junkData)).thenReturn(authECIESMessage)
        whenever(crypto.ecdhAgree(myKey, remoteKeyPoint)).thenReturn(sharedSecret)
        whenever(crypto.ellipticSign(sharedSecret.xor(nonce), ephemeralKey)).thenReturn(signature)

        PowerMockito.whenNew(AuthMessage::class.java)
                .withArguments(signature, myKey.publicKeyPoint, nonce)
                .thenReturn(authMessage)

        val authMessagePacket = encryptionHandshake.createAuthMessage()

        verify(crypto).ecdhAgree(myKey, remoteKeyPoint)
        verify(crypto).ellipticSign(sharedSecret.xor(nonce), ephemeralKey)
        verify(crypto).randomBytes(ArgumentMatchers.intThat { it >= 100 })
        verify(crypto).eciesEncrypt(remoteKeyPoint, authMessage.encoded() + junkData)

        verifyNoMoreInteractions(crypto)

        Assert.assertArrayEquals(encodedAuthECIESMessage, authMessagePacket)
    }

    @Test
    fun handleAuthAckMessage() {
        val remoteEphemeralKeyPoint: ECPoint = CryptoUtils.randomECKey().publicKeyPoint
        val eciesMessage = ECIESEncryptedMessage(ByteArray(1), ByteArray(0), ByteArray(0), ByteArray(0), ByteArray(0))

        val encodedRemoteEphemKeyPoint = remoteEphemeralKeyPoint.getEncoded(false)
        val encodedAuthAckMessage = RLP.encodeList(
                RLP.encodeElement(encodedRemoteEphemKeyPoint.copyOfRange(1, encodedRemoteEphemKeyPoint.size)),
                RLP.encodeElement(remoteNonce),
                RLP.encodeInt(4)
        )

        val authAckMessage = AuthAckMessage(encodedAuthAckMessage)

        val agreedSecret = ByteArray(32) { 4 }
        val nonceHash = ByteArray(32) { 5 }
        val sharedSecret = ByteArray(32) { 6 }
        val secretsAes = ByteArray(32) { 7 }
        val secretsMac = ByteArray(32) { 8 }
        val secretsToken = ByteArray(32) { 9 }

        whenever(crypto.eciesDecrypt(myKey.privateKey, eciesMessage)).thenReturn(encodedAuthAckMessage)
        whenever(crypto.ecdhAgree(ephemeralKey, remoteEphemeralKeyPoint)).thenReturn(agreedSecret)
        whenever(crypto.sha3(remoteNonce + nonce)).thenReturn(nonceHash)
        whenever(crypto.sha3(agreedSecret + nonceHash)).thenReturn(sharedSecret)
        whenever(crypto.sha3(agreedSecret + sharedSecret)).thenReturn(secretsAes)
        whenever(crypto.sha3(agreedSecret + secretsAes)).thenReturn(secretsMac)
        whenever(crypto.sha3(sharedSecret)).thenReturn(secretsToken)

        PowerMockito.whenNew(AuthAckMessage::class.java)
                .withArguments(encodedAuthAckMessage)
                .thenReturn(authAckMessage)

        val secrets = encryptionHandshake.handleAuthAckMessage(eciesMessage)

        verify(crypto).eciesDecrypt(myKey.privateKey, eciesMessage)
        verify(crypto).ecdhAgree(ephemeralKey, remoteEphemeralKeyPoint)
        verify(crypto).sha3(remoteNonce + nonce)
        verify(crypto).sha3(agreedSecret + nonceHash)
        verify(crypto).sha3(agreedSecret + sharedSecret)
        verify(crypto).sha3(agreedSecret + secretsAes)
        verify(crypto).sha3(sharedSecret)

        verifyNoMoreInteractions(crypto)

        val expectedEgress = KeccakDigest(256)
        expectedEgress.update(secretsMac.xor(authAckMessage.nonce), 0, secretsMac.size)
        expectedEgress.update(encodedAuthECIESMessage, 0, encodedAuthECIESMessage.size)

        val expectedEgressMac = ByteArray(expectedEgress.digestSize)
        KeccakDigest(expectedEgress).doFinal(expectedEgressMac, 0)

        val egressMac = ByteArray(secrets.egressMac.digestSize)
        KeccakDigest(secrets.egressMac).doFinal(egressMac, 0)

        Assert.assertArrayEquals(expectedEgressMac, egressMac)

        val expectedIngress = KeccakDigest(256)
        expectedIngress.update(secretsMac.xor(nonce), 0, secretsMac.size)
        expectedIngress.update(eciesMessage.encoded(), 0, eciesMessage.encoded().size)

        val expectedIngressMac = ByteArray(expectedIngress.digestSize)
        KeccakDigest(expectedIngress).doFinal(expectedIngressMac, 0)

        val ingressMac = ByteArray(secrets.ingressMac.digestSize)
        KeccakDigest(secrets.ingressMac).doFinal(ingressMac, 0)

        Assert.assertArrayEquals(expectedIngressMac, ingressMac)
    }

    @Test(expected = EncryptionHandshake.HandshakeError.InvalidAuthAckPayload::class)
    fun invalidAuthAckPayload() {
        val eciesMessage = ECIESEncryptedMessage(ByteArray(1), ByteArray(0), ByteArray(0), ByteArray(0), ByteArray(0))
        val encodedAuthAckMessage = ByteArray(0)
        whenever(crypto.eciesDecrypt(myKey.privateKey, eciesMessage)).thenReturn(encodedAuthAckMessage)

        PowerMockito.whenNew(AuthAckMessage::class.java)
                .withArguments(encodedAuthAckMessage)
                .thenThrow(Exception())

        encryptionHandshake.handleAuthAckMessage(eciesMessage)
    }
}