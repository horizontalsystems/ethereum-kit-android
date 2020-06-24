package io.horizontalsystems.ethereumkit.spv.net.connection

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.crypto.AESCipher
import io.horizontalsystems.ethereumkit.spv.rlp.RLP
import org.bouncycastle.crypto.digests.KeccakDigest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FrameCodecTest {

    private lateinit var frameCodec: FrameCodec
    private lateinit var secrets: Secrets

    private val frameCodecHelper = Mockito.mock(FrameCodecHelper::class.java)
    private val aesEncryptor = Mockito.mock(AESCipher::class.java)
    private val aesDecryptor = Mockito.mock(AESCipher::class.java)

    private val encryptedHeader = ByteArray(16) { 3 }
    private val encryptedBody = ByteArray(16) { 4 }
    private val headerMac = ByteArray(16) { 5 }
    private val bodyMac = ByteArray(16) { 6 }
    private val updatedEgressDigest = ByteArray(KeccakDigest().digestSize)

    @Before
    fun setUp() {
        secrets = Secrets(
                aes = ByteArray(16) { 0 },
                mac = ByteArray(16) { 0 },
                token = ByteArray(32) { 2 },
                egressMac = KeccakDigest(),
                ingressMac = KeccakDigest())

        val egress = KeccakDigest()
        egress.update(encryptedBody, 0, encryptedBody.size)
        egress.doFinal(updatedEgressDigest, 0)

        whenever(frameCodecHelper.updateMac(secrets.egressMac, secrets.mac, encryptedHeader)).thenReturn(headerMac)
        whenever(frameCodecHelper.updateMac(secrets.egressMac, secrets.mac, updatedEgressDigest)).thenReturn(bodyMac)
        whenever(frameCodecHelper.updateMac(secrets.ingressMac, secrets.mac, encryptedHeader)).thenReturn(headerMac)
        whenever(frameCodecHelper.updateMac(secrets.ingressMac, secrets.mac, updatedEgressDigest)).thenReturn(bodyMac)

        frameCodec = FrameCodec(secrets, frameCodecHelper, aesEncryptor, aesDecryptor)
    }

    @Test
    fun writeFrame() {
        val frame = Frame(0, ByteArray(15) { 10 })

        val frameSizeBytes = ByteArray(3) { 0 }
        val header = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0)) + ByteArray(11) { 0 }
        val body = RLP.encodeInt(frame.type) + frame.payload

        whenever(frameCodecHelper.toThreeBytes(frame.size + 1)).thenReturn(frameSizeBytes)
        whenever(aesEncryptor.process(header)).thenReturn(encryptedHeader)
        whenever(aesEncryptor.process(body)).thenReturn(encryptedBody)

        val outputStream = ByteArrayOutputStream()
        frameCodec.writeFrame(frame, outputStream)


        verify(aesEncryptor).process(header)
        verify(aesEncryptor).process(body)
        verify(frameCodecHelper).toThreeBytes(frame.size + 1)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertArrayEquals(encryptedHeader + headerMac + encryptedBody + bodyMac, outputStream.toByteArray())
    }

    @Test
    fun writeFrame_contextId() {
        val frame = Frame(0, ByteArray(15) { 10 }, 1, 1)
        val frameSizeBytes = ByteArray(3) { 0 }
        val headerInfo = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0), RLP.encodeInt(1), RLP.encodeInt(1))
        val headerPadding = ByteArray(16 - headerInfo.size) { 0 }

        val header = headerInfo + headerPadding
        val body = RLP.encodeInt(frame.type) + frame.payload

        whenever(frameCodecHelper.toThreeBytes(frame.size + 1)).thenReturn(frameSizeBytes)
        whenever(aesEncryptor.process(header)).thenReturn(encryptedHeader)
        whenever(aesEncryptor.process(body)).thenReturn(encryptedBody)

        val outputStream = ByteArrayOutputStream()
        frameCodec.writeFrame(frame, outputStream)

        verify(aesEncryptor).process(header)
        verify(aesEncryptor).process(body)
        verify(frameCodecHelper).toThreeBytes(frame.size + 1)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertArrayEquals(encryptedHeader + headerMac + encryptedBody + bodyMac, outputStream.toByteArray())
    }

    @Test
    fun writeFrame_framePadding() {
        val frame = Frame(0, ByteArray(16) { 10 })

        val frameSizeBytes = ByteArray(3) { 0 }
        val header = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0)) + ByteArray(11) { 0 }
        val body = RLP.encodeInt(frame.type) + frame.payload + ByteArray(15) { 0 }

        whenever(frameCodecHelper.toThreeBytes(frame.size + 1)).thenReturn(frameSizeBytes)
        whenever(aesEncryptor.process(header)).thenReturn(encryptedHeader)
        whenever(aesEncryptor.process(body)).thenReturn(encryptedBody)

        val outputStream = ByteArrayOutputStream()
        frameCodec.writeFrame(frame, outputStream)

        verify(aesEncryptor).process(header)
        verify(aesEncryptor).process(body)
        verify(frameCodecHelper).toThreeBytes(frame.size + 1)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.egressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertArrayEquals(encryptedHeader + headerMac + encryptedBody + bodyMac, outputStream.toByteArray())
    }

    @Test
    fun readFrame() {
        val frame = Frame(0, ByteArray(15) { 10 })

        val frameSizeBytes = ByteArray(3) { 0 }
        val header = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0)) + ByteArray(11) { 0 }
        val body = RLP.encodeInt(frame.type) + frame.payload

        whenever(frameCodecHelper.fromThreeBytes(frameSizeBytes)).thenReturn(frame.size + 1)
        whenever(aesDecryptor.process(encryptedHeader)).thenReturn(header)
        whenever(aesDecryptor.process(encryptedBody)).thenReturn(body)

        val inputStream = ByteArrayInputStream(encryptedHeader + headerMac + encryptedBody + bodyMac)

        val result = frameCodec.readFrame(inputStream)

        verify(aesDecryptor).process(encryptedHeader)
        verify(aesDecryptor).process(encryptedBody)
        verify(frameCodecHelper).fromThreeBytes(frameSizeBytes)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertNotNull(result)
        assertEquals(result!!.type, frame.type)
        assertArrayEquals(result.payload, frame.payload)
        assertEquals(result.size, frame.size)
        assertEquals(result.contextId, frame.contextId)
        assertEquals(result.totalFrameSize, frame.totalFrameSize)
    }

    @Test
    fun readFrame_contextId() {
        val frame = Frame(0, ByteArray(15) { 10 }, 1, 1)
        val frameSizeBytes = ByteArray(3) { 0 }
        val headerInfo = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0), RLP.encodeInt(1), RLP.encodeInt(1))
        val headerPadding = ByteArray(16 - headerInfo.size) { 0 }

        val header = headerInfo + headerPadding
        val body = RLP.encodeInt(frame.type) + frame.payload

        whenever(frameCodecHelper.fromThreeBytes(frameSizeBytes)).thenReturn(frame.size + 1)
        whenever(aesDecryptor.process(encryptedHeader)).thenReturn(header)
        whenever(aesDecryptor.process(encryptedBody)).thenReturn(body)

        val inputStream = ByteArrayInputStream(encryptedHeader + headerMac + encryptedBody + bodyMac)

        val result = frameCodec.readFrame(inputStream)

        verify(aesDecryptor).process(encryptedHeader)
        verify(aesDecryptor).process(encryptedBody)
        verify(frameCodecHelper).fromThreeBytes(frameSizeBytes)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertNotNull(result)
        assertEquals(result!!.type, frame.type)
        assertArrayEquals(result.payload, frame.payload)
        assertEquals(result.size, frame.size)
        assertEquals(result.contextId, frame.contextId)
        assertEquals(result.totalFrameSize, frame.totalFrameSize)
    }

    @Test
    fun readFrame_bodyPadding() {
        val frame = Frame(0, ByteArray(16) { 10 })
        val frameSizeBytes = ByteArray(3) { 0 }
        val header = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0)) + ByteArray(11) { 0 }

        val body = RLP.encodeInt(frame.type) + frame.payload

        val encryptedBody = this.encryptedBody + ByteArray(16) { 0 }
        val egress = KeccakDigest()
        egress.update(encryptedBody, 0, encryptedBody.size)
        egress.doFinal(updatedEgressDigest, 0)

        whenever(frameCodecHelper.updateMac(secrets.ingressMac, secrets.mac, updatedEgressDigest)).thenReturn(bodyMac)

        whenever(frameCodecHelper.fromThreeBytes(frameSizeBytes)).thenReturn(frame.size + 1)
        whenever(aesDecryptor.process(encryptedHeader)).thenReturn(header)
        whenever(aesDecryptor.process(encryptedBody)).thenReturn(body)

        val inputStream = ByteArrayInputStream(encryptedHeader + headerMac + encryptedBody + bodyMac)

        val result = frameCodec.readFrame(inputStream)

        verify(aesDecryptor).process(encryptedHeader)
        verify(aesDecryptor).process(encryptedBody)
        verify(frameCodecHelper).fromThreeBytes(frameSizeBytes)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, encryptedHeader)
        verify(frameCodecHelper).updateMac(secrets.ingressMac, secrets.mac, updatedEgressDigest)

        verifyNoMoreInteractions(aesEncryptor)
        verifyNoMoreInteractions(aesDecryptor)
        verifyNoMoreInteractions(frameCodecHelper)

        assertNotNull(result)
        assertEquals(result!!.type, frame.type)
        assertArrayEquals(result.payload, frame.payload)
        assertEquals(result.size, frame.size)
        assertEquals(result.contextId, frame.contextId)
        assertEquals(result.totalFrameSize, frame.totalFrameSize)
    }

    @Test(expected = FrameCodec.FrameCodecError.HeaderMacMismatch::class)
    fun readFrame_headerMacMismatch() {
        val inputStream = ByteArrayInputStream(encryptedHeader + ByteArray(16) { 11 } + encryptedBody + bodyMac)

        frameCodec.readFrame(inputStream)
    }

    @Test(expected = FrameCodec.FrameCodecError.BodyMacMismatch::class)
    fun readFrame_bodyMacMismatch() {
        val frame = Frame(0, ByteArray(15) { 10 })

        val frameSizeBytes = ByteArray(3) { 0 }
        val header = frameSizeBytes + RLP.encodeList(RLP.encodeInt(0)) + ByteArray(11) { 0 }
        val body = RLP.encodeInt(frame.type) + frame.payload

        whenever(frameCodecHelper.fromThreeBytes(frameSizeBytes)).thenReturn(frame.size + 1)
        whenever(aesDecryptor.process(encryptedHeader)).thenReturn(header)
        whenever(aesDecryptor.process(encryptedBody)).thenReturn(body)

        val inputStream = ByteArrayInputStream(encryptedHeader + headerMac + encryptedBody + ByteArray(16) { 11 })

        frameCodec.readFrame(inputStream)
    }
}