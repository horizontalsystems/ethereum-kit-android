package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.les.Peer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

class FrameHandlerTest {

    private lateinit var frameHandler: FrameHandler

    @Before
    fun setUp() {
        frameHandler = FrameHandler()
    }

    @Test
    fun getMessage() {
        val message = HelloMessage(peerId = ByteArray(64) { 0 }, port = 0, capabilities = listOf())

        frameHandler.addFrame(Frame(type = 0, payload = message.encoded()))

        val resolvedMessage = frameHandler.getMessage()

        assertNotNull(resolvedMessage)
        assertEquals(message::class, resolvedMessage!!::class)
        assertArrayEquals(message.encoded(), resolvedMessage.encoded())
    }

    @Test
    fun getMessage_EmptyFrames() {
        val resolvedMessage = frameHandler.getMessage()

        assertNull(resolvedMessage)
    }

    @Test(expected = FrameHandler.FrameHandlerError.UnknownMessageType::class)
    fun getMessage_UnknownMessageType() {
        val message = HelloMessage(peerId = ByteArray(64) { 0 }, port = 0, capabilities = listOf())

        frameHandler.addFrame(Frame(type = 5, payload = message.encoded()))

        frameHandler.getMessage()
    }

    @Test(expected = FrameHandler.FrameHandlerError.InvalidPayload::class)
    fun getMessage_InvalidPayload() {
        val message = PingMessage()

        frameHandler.addFrame(Frame(type = 0, payload = message.encoded()))

        frameHandler.getMessage()
    }

    @Test
    fun getMessage_TwoMessagesInFrames() {
        val helloMessage = HelloMessage(peerId = ByteArray(64) { 0 }, port = 0, capabilities = listOf())
        val pingMessage = PingMessage()

        frameHandler.addFrame(Frame(type = 0, payload = helloMessage.encoded()))
        frameHandler.addFrame(Frame(type = 2, payload = pingMessage.encoded()))

        val firstMessage = frameHandler.getMessage()
        val secondMessage = frameHandler.getMessage()

        assertEquals(helloMessage::class, firstMessage!!::class)
        assertEquals(pingMessage::class, secondMessage!!::class)

        assertArrayEquals(helloMessage.encoded(), firstMessage.encoded())
        assertArrayEquals(pingMessage.encoded(), secondMessage.encoded())
    }

    @Test
    fun addLesCapability() {
        val helloMessage = HelloMessage(peerId = ByteArray(64) { 0 }, port = 0, capabilities = listOf())
        val statusMessage = StatusMessage(2, 3, ByteArray(0), ByteArray(0), ByteArray(0), BigInteger.valueOf(0))

        frameHandler.register(listOf(Peer.capability))

        frameHandler.addFrame(Frame(type = 0, payload = helloMessage.encoded()))
        frameHandler.addFrame(Frame(type = 0x10, payload = statusMessage.encoded()))

        val firstMessage = frameHandler.getMessage()
        val secondMessage = frameHandler.getMessage()

        assertEquals(helloMessage::class, firstMessage!!::class)
        assertEquals(statusMessage::class, secondMessage!!::class)

        assertArrayEquals(helloMessage.encoded(), firstMessage.encoded())
        assertArrayEquals(statusMessage.encoded(), secondMessage.encoded())
    }
}