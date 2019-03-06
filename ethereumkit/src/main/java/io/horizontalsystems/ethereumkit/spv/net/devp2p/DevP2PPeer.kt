package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.connection.PeerConnection
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage

class DevP2PPeer(val connection: PeerConnection,
                 val key: ECKey, val capability: Capability,
                 val messageFactory: MessageFactory) : PeerConnection.Listener {
    interface Listener {
        fun onConnectionEstablished()
        fun onDisconnected(error: Throwable?)
        fun onMessageReceived(message: IMessage)
    }

    var listener: Listener? = null

    private fun handle(message: HelloMessage) {
        try {
            validatePeer(message)
            connection.register(listOf(capability))
            listener?.onConnectionEstablished()
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    private fun handle(message: DisconnectMessage) {
        connection.disconnect(DisconnectMessageReceived())
    }

    private fun handle(message: PingMessage) {
        connection.send(messageFactory.pongMessage())
    }

    private fun handle(message: PongMessage) {

    }

    private fun validatePeer(message: HelloMessage) {
        check(message.capabilities.contains(capability)) {
            throw PeerDoesNotSupportCapability()
        }
    }

    //------------------Public methods----------------------

    fun connect() {
        connection.connect()
    }

    fun disconnect(error: Throwable?) {
        connection.disconnect(error)
    }

    fun send(message: IMessage) {
        connection.send(message)
    }

    //-----------PeerConnection.Listener methods------------

    override fun onConnectionEstablished() {
        println("DevP2PPeer -> onConnectionEstablished \n")

        val helloMessage = messageFactory.helloMessage(key, listOf(capability))
        connection.send(helloMessage)
    }

    override fun onDisconnected(error: Throwable?) {
        println("DevP2PPeer -> onDisconnected")

        listener?.onDisconnected(error)
    }

    override fun onMessageReceived(message: IMessage) {
        println("<<<<<<< $message \n")
        when (message) {
            is HelloMessage -> handle(message)
            is DisconnectMessage -> handle(message)
            is PingMessage -> handle(message)
            is PongMessage -> handle(message)
            else -> listener?.onMessageReceived(message)
        }
    }

    open class DevP2PPeerError : Exception()
    class PeerDoesNotSupportCapability : DevP2PPeerError()
    class DisconnectMessageReceived : DevP2PPeerError()

    companion object {

        fun getInstance(node: Node, key: ECKey, capability: Capability): DevP2PPeer {
            val connection = PeerConnection(key, node)
            val devP2PPeer = DevP2PPeer(connection, key, capability, MessageFactory())
            connection.listener = devP2PPeer

            return devP2PPeer
        }
    }
}
