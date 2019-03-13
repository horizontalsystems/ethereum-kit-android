package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.MessageFactory
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage

class DevP2PPeer(val devP2PConnection: DevP2PConnection,
                 val messageFactory: MessageFactory,
                 val key: ECKey) : DevP2PConnection.Listener {
    interface Listener {
        fun didConnect()
        fun didDisconnect(error: Throwable?)
        fun didReceive(message: IMessage)
    }

    var listener: Listener? = null

    private fun handle(message: HelloMessage) {
        try {
            devP2PConnection.register(nodeCapabilities = message.capabilities)
            listener?.didConnect()
        } catch (error: Exception) {
            disconnect(error)
        }
    }

    private fun handle(message: DisconnectMessage) {
        devP2PConnection.disconnect(DisconnectMessageReceived())
    }

    private fun handle(message: PingMessage) {
        devP2PConnection.send(messageFactory.pongMessage())
    }

    private fun handle(message: PongMessage) {

    }

    //------------------Public methods----------------------

    fun connect() {
        devP2PConnection.connect()
    }

    fun disconnect(error: Throwable?) {
        devP2PConnection.disconnect(error)
    }

    fun send(message: IMessage) {
        devP2PConnection.send(message)
    }

    //-----------Connection.Listener methods------------

    override fun didConnect() {
        println("DevP2PPeer -> didConnect \n")

        val helloMessage = messageFactory.helloMessage(key, devP2PConnection.myCapabilities)
        devP2PConnection.send(helloMessage)
    }

    override fun didDisconnect(error: Throwable?) {
        println("DevP2PPeer -> didDisconnect")

        listener?.didDisconnect(error)
    }

    override fun didReceive(message: IMessage) {
        println("<<<<<<< $message \n")
        when (message) {
            is HelloMessage -> handle(message)
            is DisconnectMessage -> handle(message)
            is PingMessage -> handle(message)
            is PongMessage -> handle(message)
            else -> listener?.didReceive(message)
        }
    }

    open class DevP2PPeerError : Exception()
    class DisconnectMessageReceived : DevP2PPeerError()

    companion object {

        fun getInstance(key: ECKey, node: Node, capabilities: List<Capability>): DevP2PPeer {
            val devP2PConnection = DevP2PConnection.getInstance(capabilities, key, node)
            val devP2PPeer = DevP2PPeer(devP2PConnection, MessageFactory(), key)

            devP2PConnection.listener = devP2PPeer

            return devP2PPeer
        }
    }
}
