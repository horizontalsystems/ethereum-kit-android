package io.horizontalsystems.ethereumkit.spv.net.les

import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.ethereumkit.spv.models.BlockHeader
import io.horizontalsystems.ethereumkit.spv.net.INetwork
import io.horizontalsystems.ethereumkit.spv.net.les.messages.StatusMessage
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.math.BigInteger

class LESPeerValidatorTest {

    private var validator = LESPeerValidator()

    private var statusMessage = Mockito.mock(StatusMessage::class.java)
    private var network = Mockito.mock(INetwork::class.java)
    private var blockHeader = Mockito.mock(BlockHeader::class.java)

    @Before
    fun setUp() {
        whenever(statusMessage.networkId).thenReturn(1)
        whenever(network.id).thenReturn(1)

        whenever(statusMessage.genesisHash).thenReturn(byteArrayOf(1))
        whenever(network.genesisBlockHash).thenReturn(byteArrayOf(1))

        whenever(statusMessage.bestBlockHeight).thenReturn(BigInteger.valueOf(100))
        whenever(blockHeader.height).thenReturn(BigInteger.valueOf(99))
    }

    @Test
    fun validPeer() {
        validator.validate(statusMessage, network, blockHeader)
    }

    @Test(expected = LESPeer.WrongNetwork::class)
    fun invalidNetworkId() {
        whenever(network.id).thenReturn(2)

        validator.validate(statusMessage, network, blockHeader)
    }

    @Test(expected = LESPeer.WrongNetwork::class)
    fun invalidNetworkGenesisHash() {
        whenever(network.genesisBlockHash).thenReturn(byteArrayOf(2))

        validator.validate(statusMessage, network, blockHeader)
    }

    @Test(expected = LESPeer.ExpiredBestBlockHeight::class)
    fun expiredBestBlockHeight() {
        whenever(blockHeader.height).thenReturn(BigInteger.valueOf(101))

        validator.validate(statusMessage, network, blockHeader)
    }

}
