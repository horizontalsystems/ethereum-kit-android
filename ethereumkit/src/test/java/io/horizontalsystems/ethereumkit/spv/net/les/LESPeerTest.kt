package io.horizontalsystems.ethereumkit.spv.net.les

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
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
    private val lesPeervalidator = Mockito.mock(LESPeerValidator::class.java)
    private val listener = Mockito.mock(LESPeer.Listener::class.java)
    private val network = Mockito.mock(INetwork::class.java)
    private val lastBlockHeader = Mockito.mock(BlockHeader::class.java)

    @Before
    fun setUp() {
        lesPeer = LESPeer(devP2PPeer, messageFactory, lesPeervalidator, network, lastBlockHeader)
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
    fun didConnect() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)

        whenever(messageFactory.statusMessage(network, lastBlockHeader)).thenReturn(statusMessage)

        lesPeer.didConnect()

        verify(devP2PPeer).send(statusMessage)
    }

    @Test
    fun didReceive_statusMessage() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)

        whenever(lesPeervalidator.validate(statusMessage, network, lastBlockHeader)).then { }

        lesPeer.didReceive(statusMessage)

        verify(listener).didConnect()
    }

    @Test
    fun didReceive_invalidStatusMessage() {
        val statusMessage = Mockito.mock(StatusMessage::class.java)
        val validationError = Exception()

        whenever(lesPeervalidator.validate(statusMessage, network, lastBlockHeader)).thenThrow(validationError)

        lesPeer.didReceive(statusMessage)

        verify(devP2PPeer).disconnect(validationError)
        verifyNoMoreInteractions(devP2PPeer)
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun didReceive_blockHeadersMessage() {
        val blockHeadersMessage = Mockito.mock(BlockHeadersMessage::class.java)
        val blockHeaders = mutableListOf<BlockHeader>()

        whenever(blockHeadersMessage.headers).thenReturn(blockHeaders)

        lesPeer.didReceive(blockHeadersMessage)

        verify(listener).didReceive(blockHeaders)
    }
}
