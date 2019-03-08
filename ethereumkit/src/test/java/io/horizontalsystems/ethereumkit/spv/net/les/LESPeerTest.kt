package io.horizontalsystems.ethereumkit.spv.net.les

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.devp2p.DevP2PPeer
import io.horizontalsystems.ethereumkit.spv.net.les.messages.BlockHeadersMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.GetBlockHeadersMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.GetProofsMessage
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class LESPeerTest {

    private lateinit var lesPeer: LESPeer

    private val devP2PPeer = Mockito.mock(DevP2PPeer::class.java)
    private val messageFactory = Mockito.mock(MessageFactory::class.java)
    private val statusHandler = Mockito.mock(StatusHandler::class.java)
    private val listener = Mockito.mock(LESPeer.Listener::class.java)
    @Before
    fun setUp() {
        lesPeer = LESPeer(devP2PPeer, messageFactory, statusHandler)
        lesPeer.listener = listener
    }

    @Test
    fun connect() {
        lesPeer.connect()

        verify(devP2PPeer).connect()
    }

    @Test
    fun disconnect() {
        val error = Mockito.mock(Throwable::class.java)
        lesPeer.disconnect(error)

        verify(devP2PPeer).disconnect(error)
    }

    @Test
    fun requestBlockHeadersFrom() {
        val blockHash = ByteArray(32) { 0 }
        val getBlockHeadersMessage = Mockito.mock(GetBlockHeadersMessage::class.java)

        whenever(messageFactory.getBlockHeadersMessage(blockHash)).thenReturn(getBlockHeadersMessage)

        lesPeer.requestBlockHeadersFrom(blockHash)

        verify(devP2PPeer).send(getBlockHeadersMessage)
    }

    @Test
    fun requestProofs() {
        val address = ByteArray(32) { 0 }
        val blockHash = ByteArray(32) { 1 }
        val getProofsMessage = Mockito.mock(GetProofsMessage::class.java)

        whenever(messageFactory.getProofsMessage(address, blockHash)).thenReturn(getProofsMessage)

        lesPeer.requestProofs(address, blockHash)

        verify(devP2PPeer).send(getProofsMessage)
    }

    @Test
    fun onConnectionEstablished() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)
        val network = Mockito.mock(INetwork::class.java)
        val blockHeader = Mockito.mock(BlockHeader::class.java)

        whenever(statusHandler.network).thenReturn(network)
        whenever(statusHandler.blockHeader).thenReturn(blockHeader)
        whenever(messageFactory.statusMessage(network, blockHeader)).thenReturn(statusMessage)

        lesPeer.didConnect()

        verify(devP2PPeer).send(statusMessage)
    }

    @Test
    fun onStatusReceived() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)

        whenever(statusHandler.validate(statusMessage)).then { }

        lesPeer.didReceive(statusMessage)

        verify(listener).didConnect()
    }

    @Test
    fun onStatusReceived_invalid() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)
        val validationError = Exception()

        whenever(statusHandler.validate(statusMessage)).thenThrow(validationError)

        lesPeer.didReceive(statusMessage)

        verify(devP2PPeer).disconnect(validationError)
    }

    @Test
    fun onBlockHeadersReceived() {
        val blockHeadersMessage = Mockito.mock(BlockHeadersMessage::class.java)
        val blockHeaders = mutableListOf<BlockHeader>()

        whenever(blockHeadersMessage.headers).thenReturn(blockHeaders)

        lesPeer.didReceive(blockHeadersMessage)

        verify(listener).didReceive(blockHeaders)
    }
}
