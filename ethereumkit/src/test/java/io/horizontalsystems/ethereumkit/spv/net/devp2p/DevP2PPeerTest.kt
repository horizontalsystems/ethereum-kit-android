package io.horizontalsystems.ethereumkit.spv.net.devp2p

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DevP2PPeerTest {

    private lateinit var devP2PPeer: DevP2PPeer

    private val devP2PConnection = Mockito.mock(DevP2PConnection::class.java)
    private val key = Mockito.mock(ECKey::class.java)
    private val capability = Mockito.mock(Capability::class.java)
    private val messageFactory = Mockito.mock(MessageFactory::class.java)
    private val listener = Mockito.mock(DevP2PPeer.Listener::class.java)

    @Before
    fun setUp() {
        devP2PPeer = DevP2PPeer(devP2PConnection, messageFactory, key)
        devP2PPeer.listener = listener
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun connect() {
        devP2PPeer.connect()

        verify(devP2PConnection).connect()
    }

    @Test
    fun disconnect() {
        val error = Mockito.mock(Throwable::class.java)
        devP2PPeer.disconnect(error)

        verify(devP2PConnection).disconnect(error)
    }

    @Test
    fun send() {
        val message = Mockito.mock(IMessage::class.java)
        devP2PPeer.send(message)

        verify(devP2PConnection).send(message)
    }

    @Test
    fun onConnectionEstablished() {
        val helloMessage = Mockito.mock(HelloMessage::class.java)

        whenever(devP2PConnection.myCapabilities).thenReturn(listOf(capability))
        whenever(messageFactory.helloMessage(key, listOf(capability))).thenReturn(helloMessage)

        devP2PPeer.didConnect()

        verify(devP2PConnection).send(helloMessage)
    }

    @Test
    fun onDisconnected() {
        val error = Mockito.mock(Throwable::class.java)

        devP2PPeer.didDisconnect(error)

        verify(listener).didDisconnect(error)
    }

    @Test
    fun onHelloReceived() {
        val message = Mockito.mock(HelloMessage::class.java)
        val capabilities = listOf(capability)
        whenever(message.capabilities).thenReturn(capabilities)

        devP2PPeer.didReceive(message)

        verify(devP2PConnection).register(capabilities)
        verify(listener).didConnect()
    }

    @Test
    fun onHelloReceived_NoCapability() {
        val message = Mockito.mock(HelloMessage::class.java)
        val capabilities = listOf<Capability>()

        whenever(message.capabilities).thenReturn(capabilities)
        whenever(devP2PConnection.register(capabilities)).thenThrow(DevP2PConnection.NoCommonCapabilities())

        devP2PPeer.didReceive(message)

        verify(devP2PConnection).disconnect(argThat { this is DevP2PConnection.NoCommonCapabilities })
    }

    @Test
    fun onDisconnectReceived() {
        val message = Mockito.mock(DisconnectMessage::class.java)

        devP2PPeer.didReceive(message)

        verify(devP2PConnection).disconnect(argThat { this is DevP2PPeer.DisconnectMessageReceived })
    }

    @Test
    fun onPingReceived() {
        val pingMessage = Mockito.mock(PingMessage::class.java)
        val pongMessage = Mockito.mock(PongMessage::class.java)
        whenever(messageFactory.pongMessage()).thenReturn(pongMessage)

        devP2PPeer.didReceive(pingMessage)

        verify(devP2PConnection).send(pongMessage)
    }

    @Test
    fun onPongReceived() {
        val message = Mockito.mock(PongMessage::class.java)

        devP2PPeer.didReceive(message)

        verifyNoMoreInteractions(devP2PConnection)
        verifyNoMoreInteractions(listener)
    }
}
