package io.horizontalsystems.ethereumkit.spv.net.les

/*
class LESPeerTest : Spek({

    lateinit var lesPeer: LESPeer

    val devP2PPeer = mock(DevP2PPeer::class.java)
    val network = mock(INetwork::class.java)
    val lastBlockHeader = mock(BlockHeader::class.java)
    val randomHelper = mock(RandomHelper::class.java)
    val requestHolder = mock(LESPeerRequestHolder::class.java)
    val listener = mock(IPeerListener::class.java)

    val protocolVersion = LESPeer.capability.version
    val networkId = 1
    val genesisHash = ByteArray(32) { 1 }
    val headTotalDifficulty = BigInteger("12345")
    val headHash = ByteArray(32) { 3 }
    val headHeight = 100L

    beforeEachTest {
        whenever(network.id).thenReturn(networkId)
        whenever(network.genesisBlockHash).thenReturn(genesisHash)

        whenever(lastBlockHeader.totalDifficulty).thenReturn(headTotalDifficulty)
        whenever(lastBlockHeader.hashHex).thenReturn(headHash)
        whenever(lastBlockHeader.height).thenReturn(headHeight)

        lesPeer = LESPeer(devP2PPeer, network, lastBlockHeader, randomHelper, requestHolder)
        lesPeer.listener = listener
    }

    afterEachTest {
        reset(devP2PPeer, network, lastBlockHeader, randomHelper, requestHolder, listener)
    }

    describe("#connect") {
        it("connects devP2P peer") {
            lesPeer.connect()

            verify(devP2PPeer).connect()
        }
    }

    describe("#disconnect") {
        it("disconnects devP2P peer") {
            val error = mock(Throwable::class.java)
            lesPeer.disconnect(error)

            verify(devP2PPeer).disconnect(error)
        }
    }

    describe("#didconnect") {
        beforeEach {
            lesPeer.didConnect()
        }

        it("sends status message") {
            verify(devP2PPeer).send(argThat {
                this is StatusMessage &&
                        this.protocolVersion == protocolVersion &&
                        this.networkId == networkId &&
                        Arrays.equals(this.genesisHash, genesisHash) &&
                        this.headTotalDifficulty == headTotalDifficulty &&
                        Arrays.equals(this.headHash, headHash) &&
                        this.headHeight == headHeight
            })
        }
    }

    describe("#didDisconnect") {
        it("notifies listener") {
            val error = mock(Throwable::class.java)
            lesPeer.didDisconnect(error)

            verify(listener).didDisconnect(error)
        }
    }

    describe("#didReceive") {
        context("when message is StatusMessage") {
            val statusMessage = mock(StatusMessage::class.java)

            beforeEach {
                whenever(statusMessage.protocolVersion).thenReturn(protocolVersion)
                whenever(statusMessage.networkId).thenReturn(networkId)
                whenever(statusMessage.genesisHash).thenReturn(genesisHash)
                whenever(statusMessage.headHeight).thenReturn(headHeight)
            }

            context("when valid") {
                it("notifies listener that connected") {
                    lesPeer.didReceive(statusMessage)
                    verify(listener).didConnect()
                }
            }

            context("when invalid") {

                it("does not notify listener that connected") {
                    verifyNoMoreInteractions(listener)
                }

                context("protocolVersion") {
                    beforeEach {
                        whenever(statusMessage.protocolVersion).thenReturn(3.toByte())
                        lesPeer.didReceive(statusMessage)
                    }

                    it("disconnects with InvalidProtocolVersion exception") {
                        verify(devP2PPeer).disconnect(argThat { this is LESPeer.InvalidProtocolVersion })
                    }
                }

                context("networkId") {
                    beforeEach {
                        whenever(statusMessage.networkId).thenReturn(2)
                        lesPeer.didReceive(statusMessage)
                    }

                    it("disconnects with WrongNetwork exception") {
                        verify(devP2PPeer).disconnect(argThat { this is LESPeer.WrongNetwork })
                    }
                }

                context("genesisBlockHash") {
                    beforeEach {
                        whenever(statusMessage.genesisHash).thenReturn(ByteArray(32) { 0 })
                        lesPeer.didReceive(statusMessage)
                    }

                    it("disconnects with WrongNetwork exception") {
                        verify(devP2PPeer).disconnect(argThat { this is LESPeer.WrongNetwork })
                    }
                }

                context("headHeight") {
                    beforeEach {
                        whenever(statusMessage.headHeight).thenReturn(99L)
                        lesPeer.didReceive(statusMessage)
                    }

                    it("disconnects with ExpiredBestBlockHeight exception") {
                        verify(devP2PPeer).disconnect(argThat { this is LESPeer.ExpiredBestBlockHeight })
                    }
                }
            }
        }

        context("when message is BlockHeadersMessage") {
            val blockHeadersMessage = mock(BlockHeadersMessage::class.java)
            val headers = listOf<BlockHeader>()
            val requestId = 123L

            beforeEach {
                whenever(blockHeadersMessage.headers).thenReturn(headers)
                whenever(blockHeadersMessage.requestID).thenReturn(requestId)
            }

            afterEach {
                reset(requestHolder)
            }

            context("when request exists in holder") {
                val blockHeaderRequest = mock(BlockHeaderRequest::class.java)
                val blockHeader = mock(BlockHeader::class.java)

                beforeEach {
                    whenever(requestHolder.removeBlockHeaderRequest(requestId)).thenReturn(blockHeaderRequest)
                    whenever(blockHeaderRequest.blockHeader).thenReturn(blockHeader)
                }

                context("when in reversed order") {
                    beforeEach {
                        whenever(blockHeaderRequest.reversed).thenReturn(true)
                    }
                    it("notifies listener with reversed flag true") {
                        lesPeer.didReceive(blockHeadersMessage)
                        verify(listener).didReceive(headers, blockHeader, true)
                    }
                }

                context("when in not reversed order") {
                    beforeEach {
                        whenever(blockHeaderRequest.reversed).thenReturn(false)
                    }
                    it("notifies listener with reversed flag true") {
                        lesPeer.didReceive(blockHeadersMessage)
                        verify(listener).didReceive(headers, blockHeader, false)
                    }
                }
            }

            context("when request does not exist in holder") {
                beforeEach {
                    whenever(requestHolder.removeBlockHeaderRequest(requestId)).thenReturn(null)

                    lesPeer.didReceive(blockHeadersMessage)
                }

                it("disconnects with UnexpectedMessage exception") {
                    verify(devP2PPeer).disconnect(argThat { this is LESPeer.UnexpectedMessage })
                }

                it("does not notify listener") {
                    verifyNoMoreInteractions(listener)
                }
            }
        }

        context("when message is ProofsMessage") {
            val proofsMessage = mock(ProofsMessage::class.java)
            val requestId = 123L

            beforeEach {
                whenever(proofsMessage.requestID).thenReturn(requestId)
            }

            context("when request exists in holder") {
                val accountStateRequest = mock(AccountStateRequest::class.java)
                val address = ByteArray(10) { 1 }
                val blockHeader = mock(BlockHeader::class.java)
                val accountState = mock(AccountState::class.java)

                beforeEach {
                    whenever(requestHolder.removeAccountStateRequest(requestId)).thenReturn(accountStateRequest)
                    whenever(accountStateRequest.address).thenReturn(address)
                    whenever(accountStateRequest.blockHeader).thenReturn(blockHeader)
                    whenever(accountStateRequest.getAccountState(proofsMessage)).thenReturn(accountState)

                    lesPeer.didReceive(proofsMessage)
                }

                it("notifies listener") {
                    verify(listener).didReceive(eq(accountState), argThat { this.contentEquals(address) }, eq(blockHeader))
                }
            }

            context("when request does not exist in holder") {

                beforeEach {
                    whenever(requestHolder.removeAccountStateRequest(requestId)).thenReturn(null)

                    lesPeer.didReceive(proofsMessage)
                }

                it("disconnects with UnexpectedMessage exception") {
                    verify(devP2PPeer).disconnect(argThat { this is LESPeer.UnexpectedMessage })
                }

                it("does not notify listener") {
                    verifyNoMoreInteractions(listener)
                }
            }
        }

        context("when message is AnnounceMessage") {
            val announceMessage = mock(AnnounceMessage::class.java)
            val blockHash = ByteArray(32) { 1 }
            val blockHeight = 1234L

            beforeEach {
                whenever(announceMessage.blockHash).thenReturn(blockHash)
                whenever(announceMessage.blockHeight).thenReturn(blockHeight)

                lesPeer.didReceive(announceMessage)
            }

            it("notifies the listener") {
                verify(listener).didAnnounce(blockHash, blockHeight)
            }
        }
    }

    describe("#requestBlockHeaders") {
        val blockHeader = mock(BlockHeader::class.java)
        val blockHeight = 123456L
        val requestId = 123L
        val limit = 100

        beforeEach {
            whenever(blockHeader.height).thenReturn(blockHeight)
            whenever(randomHelper.randomLong()).thenReturn(requestId)

            lesPeer.requestBlockHeaders(blockHeader, limit)
        }

        it("sets request to holder") {
            verify(requestHolder).setBlockHeaderRequest(argThat { this.blockHeader == blockHeader }, eq(requestId))
        }

        it("sends message to devP2PPeer") {
            verify(devP2PPeer).send(argThat {
                this is GetBlockHeadersMessage &&
                        this.requestID == requestId &&
                        this.blockHeight == blockHeight &&
                        this.maxHeaders == limit
            })
        }
    }

    describe("#requestAccountState") {
        val requestId = 123L
        val blockHash = ByteArray(32) { 9 }
        val address = ByteArray(32) { 10 }
        val blockHeader = mock(BlockHeader::class.java)

        beforeEach {
            whenever(randomHelper.randomLong()).thenReturn(requestId)
            whenever(blockHeader.hashHex).thenReturn(blockHash)

            lesPeer.requestAccountState(address, blockHeader)
        }

        it("sets request to holder") {
            verify(requestHolder).setAccountStateRequest(
                    argThat { this.address.contentEquals(address) && this.blockHeader == blockHeader }, eq(requestId))
        }

        it("sends message to devP2P peer") {
            verify(devP2PPeer).send(argThat {
                this is GetProofsMessage &&
                        this.requestID == requestId &&
                        this.proofRequests.size == 1 &&
                        this.proofRequests[0].key.contentEquals(address) &&
                        this.proofRequests[0].blockHash.contentEquals(blockHash)
            })
        }
    }


})
*/
