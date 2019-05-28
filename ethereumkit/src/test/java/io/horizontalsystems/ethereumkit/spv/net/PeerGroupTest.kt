package io.horizontalsystems.ethereumkit.spv.net

/*
class PeerGroupTest : Spek({

    lateinit var peerGroup: PeerGroup

    val listener = mock(PeerGroup.Listener::class.java)
    val storage = mock(SpvStorage::class.java)
    val peerProvider = mock(PeerProvider::class.java)
    val blockValidator = mock(BlockValidator::class.java)
    val blockHelper = mock(BlockHelper::class.java)
    val state = mock(PeerGroupState::class.java)
    val syncPeer = mock(LESPeer::class.java)
    val address = ByteArray(64) { 6 }
    val headersLimit = 2

    val lastBlockHeader = mock(BlockHeader::class.java)
    val lastBlockHeight = 123L

    beforeEachTest {
        whenever(state.syncPeer).thenReturn(syncPeer)

        whenever(blockHelper.lastBlockHeader).thenReturn(lastBlockHeader)
        whenever(lastBlockHeader.height).thenReturn(lastBlockHeight)

        peerGroup = PeerGroup(storage, peerProvider, blockValidator, blockHelper, state, address, headersLimit)
        peerGroup.listener = listener
    }

    afterEachTest {
        reset(storage, peerProvider, blockValidator, state, syncPeer, listener)
    }

    describe("#syncState") {
        beforeEach {
            whenever(state.syncState).thenReturn(EthereumKit.SyncState.Syncing)
        }
        it("returns syncState from state") {
            assertEquals(state.syncState, peerGroup.syncState)
        }
    }

    describe("#start") {
        beforeEach {
            whenever(peerProvider.getPeer()).thenReturn(syncPeer)

            peerGroup.start()
        }

        afterEach {
            reset(syncPeer)
        }

        it("sets itself as listener to peer") {
            verify(syncPeer).listener = peerGroup
        }

        it("sets sync peer to state") {
            verify(state).syncPeer = syncPeer
        }
        it("sets syncing to state") {
            verify(state).syncState = EthereumKit.SyncState.Syncing
        }

        it("notifies listener that sync state changed to syncing") {
            verify(listener).onUpdate(EthereumKit.SyncState.Syncing)
        }

        it("connects to peer") {
            verify(syncPeer).connect()
        }
    }

    describe("#didConnect") {

        it("calls requestBlockHeaders") {
            peerGroup.didConnect()
            verify(syncPeer).requestBlockHeaders(lastBlockHeader, headersLimit)
        }
    }

    describe("#didDisconnect") {
        it("sets syncPeer to null") {
            peerGroup.didDisconnect(null)
            verify(state).syncPeer = null
        }
    }

    describe("#didReceiveBlockHeaders") {
        val firstBlockHeader = mock(BlockHeader::class.java)
        val secondBlockHeader = mock(BlockHeader::class.java)

        val fromBlockHeader = mock(BlockHeader::class.java)

        context("when block headers are valid") {

            val receivedBlockHeaders = listOf(firstBlockHeader, secondBlockHeader)

            beforeEach {
                whenever(blockValidator.validate(receivedBlockHeaders, lastBlockHeader)).then { }
            }

            afterEach {
                reset(blockValidator, storage)
            }

            it("validates chain") {
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
                verify(blockValidator).validate(receivedBlockHeaders, fromBlockHeader)
            }

            it("saves all block headers to storage") {
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
                verify(storage).saveBlockHeaders(receivedBlockHeaders)
            }

            context("when blocks count is the same as limit") {
                it("requests more headers") {
                    peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
                    verify(syncPeer).requestBlockHeaders(secondBlockHeader, headersLimit)
                }
            }

            context("when blocks count is smaller than limit") {
                it("requests account state") {
                    peerGroup.didReceive(listOf(firstBlockHeader), fromBlockHeader)
                    verify(syncPeer).requestAccountState(address, lastBlockHeader)
                }
            }
        }

        context("when chain is invalid") {
            val receivedBlockHeaders = listOf(firstBlockHeader, secondBlockHeader)

            beforeEach {
                whenever(blockValidator.validate(receivedBlockHeaders, fromBlockHeader)).thenThrow(BlockValidator.InvalidChain())
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
            }

            it("disconnects peer with InvalidChain error") {
                verify(syncPeer).disconnect(argThat { this is BlockValidator.InvalidChain })
            }
        }

        context("when invalid proof of work") {
            val receivedBlockHeaders = listOf(firstBlockHeader, secondBlockHeader)

            beforeEach {
                whenever(blockValidator.validate(receivedBlockHeaders, fromBlockHeader)).thenThrow(BlockValidator.InvalidProofOfWork())
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
            }

            it("disconnects peer with InvalidProofOfWork error") {
                verify(syncPeer).disconnect(argThat { this is BlockValidator.InvalidProofOfWork })
            }
        }

        context("when fork detected") {
            val receivedBlockHeaders = listOf(firstBlockHeader, secondBlockHeader)

            beforeEach {
                whenever(blockValidator.validate(receivedBlockHeaders, fromBlockHeader)).thenThrow(BlockValidator.ForkDetected())
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader)
            }

            it("requests block headers in reverse") {
                verify(syncPeer).requestBlockHeaders(fromBlockHeader, headersLimit, true)
            }
        }
    }

    describe("#didReceiveBlockHeaders-reversed") {
        val block1 = mock(BlockHeader::class.java)
        val block1HashHex = ByteArray(32) { 1 }
        val block2 = mock(BlockHeader::class.java)
        val block2HashHex = ByteArray(32) { 2 }

        val forkedBlock1 = mock(BlockHeader::class.java)
        val forkedBlock1HashHex = ByteArray(32) { 3 }
        val forkedBlock2 = mock(BlockHeader::class.java)
        val forkedBlock2HashHex = ByteArray(32) { 4 }

        val fromBlockHeader = mock(BlockHeader::class.java)
        val fromBlockHeight = 100L

        val receivedBlockHeaders = listOf(block2, block1)

        beforeEach {
            whenever(fromBlockHeader.height).thenReturn(fromBlockHeight)

            whenever(block1.hashHex).thenReturn(block1HashHex)
            whenever(block1.height).thenReturn(1L)

            whenever(block2.hashHex).thenReturn(block2HashHex)
            whenever(block2.height).thenReturn(2L)

            whenever(forkedBlock1.hashHex).thenReturn(forkedBlock1HashHex)
            whenever(forkedBlock1.height).thenReturn(1L)

            whenever(forkedBlock2.hashHex).thenReturn(forkedBlock2HashHex)
            whenever(forkedBlock2.height).thenReturn(2L)
        }

        afterEach {
            reset(block1, block2, forkedBlock1, forkedBlock2, fromBlockHeader)
        }

        context("when found forked block") {
            beforeEach {
                whenever(storage.getBlockHeadersReversed(fromBlockHeight, receivedBlockHeaders.size)).thenReturn(listOf(forkedBlock2, block1))
            }

            it("requests block headers starting from forked block") {
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader, true)

                verify(syncPeer).requestBlockHeaders(block1, headersLimit, false)
            }
        }

        context("when not found forked block") {
            beforeEach {
                whenever(storage.getBlockHeadersReversed(fromBlockHeight, receivedBlockHeaders.size)).thenReturn(listOf(forkedBlock2, forkedBlock1))
            }

            it("disconnects peer with exception InvalidPeer") {
                peerGroup.didReceive(receivedBlockHeaders, fromBlockHeader, true)

                verify(syncPeer).disconnect(argThat { this is PeerGroup.InvalidPeer })
            }
        }
    }

    describe("#didReceiveAccountState") {
        val accountState = mock(AccountState::class.java)
        val blockHeader = mock(BlockHeader::class.java)

        it("notifies listener") {
            peerGroup.didReceive(accountState, address, blockHeader)

            verify(listener).onUpdate(accountState)
        }

        it("sets syncState to Synced") {
            peerGroup.didReceive(accountState, address, blockHeader)

            verify(state).syncState = EthereumKit.SyncState.Synced
        }

        it("notifies listener that sync state changed to Synced") {
            peerGroup.didReceive(accountState, address, blockHeader)

            verify(listener).onUpdate(EthereumKit.SyncState.Synced)
        }
    }


    describe("#didAnounce") {
        val blockHash = ByteArray(32) { 7 }
        val blockHeight = 1234565L

        context("when syncState is NOT Synced") {
            beforeEach {
                whenever(state.syncState).thenReturn(EthereumKit.SyncState.Syncing)
            }

            it("does nothing") {
                peerGroup.didAnnounce(blockHash, blockHeight)

                verifyNoMoreInteractions(syncPeer)
            }
        }

        context("when syncState is Synced") {
            beforeEach {
                whenever(state.syncState).thenReturn(EthereumKit.SyncState.Synced)
            }

            it("requests block headers from last block header") {
                peerGroup.didAnnounce(blockHash, blockHeight)

                verify(syncPeer).requestBlockHeaders(lastBlockHeader, headersLimit)
            }
        }
    }
})
*/
