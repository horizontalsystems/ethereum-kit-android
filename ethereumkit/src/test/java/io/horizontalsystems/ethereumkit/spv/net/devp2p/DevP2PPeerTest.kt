package io.horizontalsystems.ethereumkit.spv.net.devp2p

import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.connection.PeerConnection
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

    private val connection = Mockito.mock(PeerConnection::class.java)
    private val key = Mockito.mock(ECKey::class.java)
    private val capability = Mockito.mock(Capability::class.java)
    private val messageFactory = Mockito.mock(MessageFactory::class.java)
    private val listener = Mockito.mock(DevP2PPeer.Listener::class.java)

    @Before
    fun setUp() {
        devP2PPeer = DevP2PPeer(connection, key, capability, messageFactory)
        devP2PPeer.listener = listener
    }

    @After
    fun tearDown() {
        verifyNoMoreInteractions(connection)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun connect() {
        devP2PPeer.connect()

        verify(connection).connect()
    }

    @Test
    fun disconnect() {
        val error = Mockito.mock(Throwable::class.java)
        devP2PPeer.disconnect(error)

        verify(connection).disconnect(error)
    }

    @Test
    fun send() {
        val message = Mockito.mock(IMessage::class.java)
        devP2PPeer.send(message)

        verify(connection).send(message)
    }

    @Test
    fun onConnectionEstablished() {
        val helloMessage = Mockito.mock(HelloMessage::class.java)
        whenever(messageFactory.helloMessage(key, listOf(capability))).thenReturn(helloMessage)

        devP2PPeer.onConnectionEstablished()

        verify(connection).send(helloMessage)
    }

    @Test
    fun onDisconnected() {
        val error = Mockito.mock(Throwable::class.java)

        devP2PPeer.onDisconnected(error)

        verify(listener).onDisconnected(error)
    }

    @Test
    fun onHelloReceived() {
        val message = Mockito.mock(HelloMessage::class.java)
        val capabilities = listOf(capability)
        whenever(message.capabilities).thenReturn(capabilities)

        devP2PPeer.onMessageReceived(message)

        verify(connection).register(capabilities)
        verify(listener).onConnectionEstablished()
    }

    @Test
    fun onHelloReceived_NoCapability() {
        val message = Mockito.mock(HelloMessage::class.java)
        val capabilities = listOf<Capability>()

        whenever(message.capabilities).thenReturn(capabilities)

        devP2PPeer.onMessageReceived(message)

        verify(connection).disconnect(argThat { this is DevP2PPeer.PeerDoesNotSupportCapability })
    }

    @Test
    fun onDisconnectReceived() {
        val message = Mockito.mock(DisconnectMessage::class.java)

        devP2PPeer.onMessageReceived(message)

        verify(connection).disconnect(argThat { this is DevP2PPeer.DisconnectMessageReceived })
    }

    @Test
    fun onPingReceived() {
        val pingMessage = Mockito.mock(PingMessage::class.java)
        val pongMessage = Mockito.mock(PongMessage::class.java)
        whenever(messageFactory.pongMessage()).thenReturn(pongMessage)

        devP2PPeer.onMessageReceived(pingMessage)

        verify(connection).send(pongMessage)
    }

    @Test
    fun onPongReceived() {
        val message = Mockito.mock(PongMessage::class.java)

        devP2PPeer.onMessageReceived(message)

        verifyNoMoreInteractions(connection)
        verifyNoMoreInteractions(listener)
    }
}
