package io.horizontalsystems.ethereumkit.spv.net.devp2p

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import org.mockito.Mockito.mock
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class DevP2PPeerTest : Spek({

    lateinit var devP2PPeer: DevP2PPeer

    val devP2PConnection = mock(DevP2PConnection::class.java)
    val capabilityHelper = mock(CapabilityHelper::class.java)
    val myCapabilities = listOf(Capability("les", 2))
    val myNodeId = ByteArray(64) { 3 }
    val port = 30303
    val listener = mock(DevP2PPeer.Listener::class.java)

    beforeEachTest {
        devP2PPeer = DevP2PPeer(devP2PConnection, capabilityHelper, myCapabilities, myNodeId, port)
        devP2PPeer.listener = listener
    }

    afterEachTest {
        reset(devP2PConnection, capabilityHelper, listener)
    }

    describe("#connect") {
        beforeEachTest {
            devP2PPeer.connect()
        }

        it("connects devP2P connection") {
            verify(devP2PConnection).connect()
        }
    }

    describe("#disconnect") {
        val error = Exception()
        beforeEachTest {
            devP2PPeer.disconnect(error)
        }

        it("disconnects devP2P connection") {
            verify(devP2PConnection).disconnect(error)
        }
    }

    describe("#sendMessage") {
        val message = mock(IOutMessage::class.java)
        beforeEachTest {
            devP2PPeer.send(message)
        }

        it("sends message using devP2P connection") {
            verify(devP2PConnection).send(message)
        }
    }

    describe("#didConnect") {
        beforeEachTest {
            devP2PPeer.didConnect()
        }

        it("sends HelloMessage using devP2P connection") {
            verify(devP2PConnection).send(argThat {
                this is HelloMessage &&
                        this.nodeId.contentEquals(myNodeId) &&
                        this.port == port &&
                        this.capabilities == myCapabilities
            })
        }
    }

    describe("#didDisconnect") {
        val error = Exception()
        beforeEachTest {
            devP2PPeer.didDisconnect(error)
        }

        it("notifies listener") {
            verify(listener).didDisconnect(error)
        }
    }

    describe("#didReceiveMessage") {

        context("when message is HelloMessage") {
            val helloMessage = mock(HelloMessage::class.java)
            val nodeCapabilities = listOf(Capability("eth", 63))
            beforeEachTest {
                whenever(helloMessage.capabilities).thenReturn(nodeCapabilities)
            }

            context("when has no shared capabilities") {
                beforeEachTest {
                    whenever(capabilityHelper.sharedCapabilities(myCapabilities, nodeCapabilities)).thenReturn(listOf())

                    devP2PPeer.didReceive(helloMessage)
                }

                it("disconnects with NoSharedCapabilities error") {
                    verify(devP2PConnection).disconnect(argThat {
                        this is DevP2PPeer.NoSharedCapabilities
                    })
                }
            }

            context("when has shared capabilities") {
                val sharedCapabilities = listOf(myCapabilities[0])

                beforeEachTest {
                    whenever(capabilityHelper.sharedCapabilities(myCapabilities, nodeCapabilities)).thenReturn(sharedCapabilities)

                    devP2PPeer.didReceive(helloMessage)
                }

                it("registers shared capabilities to connection") {
                    verify(devP2PConnection).register(sharedCapabilities)
                }

                it("notifies listener that did connect") {
                    verify(listener).didConnect()
                }
            }
        }

        context("when message is DisconnectMessage") {
            val disconnectMessage = mock(DisconnectMessage::class.java)

            beforeEachTest {
                devP2PPeer.didReceive(disconnectMessage)
            }

            it("disconnects with disconnectMessageReceived error") {
                verify(devP2PConnection).disconnect(argThat { this is DevP2PPeer.DisconnectMessageReceived })
            }
        }

        context("when message is PingMessage") {
            val pingMessage = mock(PingMessage::class.java)

            beforeEachTest {
                devP2PPeer.didReceive(pingMessage)
            }

            it("sends PongMessage to devP2P connection") {
                verify(devP2PConnection).send(argThat { this is PongMessage })
            }
        }

        context("when message is PongMessage") {
            val pongMessage = mock(PongMessage::class.java)

            beforeEachTest {
                devP2PPeer.didReceive(pongMessage)
            }

            it("does not notify delegate") {
                verifyNoMoreInteractions(listener)
            }
        }

        context("when message is another message") {
            val message = mock(IInMessage::class.java)

            beforeEachTest {
                devP2PPeer.didReceive(message)
            }

            it("notifies delegate that message is received") {
                verify(listener).didReceive(message)
            }
        }
    }


})
