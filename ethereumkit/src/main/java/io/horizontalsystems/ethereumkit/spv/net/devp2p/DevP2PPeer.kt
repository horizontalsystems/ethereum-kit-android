package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.connection.Connection
import io.horizontalsystems.ethereumkit.spv.net.connection.IPeerConnection
import io.horizontalsystems.ethereumkit.spv.net.connection.IPeerConnectionListener
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import java.util.concurrent.Executors

interface IDevP2PPeerListener {
    fun onConnectionEstablished()
    fun onDisconnected(error: Throwable?)
    fun onMessageReceived(message: IMessage)
}

class DevP2PPeer(val key: ECKey, val node: Node, val capability: Capability, val listener: IDevP2PPeerListener) : IPeerConnectionListener {

    private var connection: IPeerConnection = Connection(node, this)
    private val executor = Executors.newSingleThreadExecutor()

    var helloSent = false
    var helloReceived = false

    private fun proceedHandshake() {
        if (helloSent) {
            if (helloReceived) {
                connection.register(listOf(capability))
                listener.onConnectionEstablished()
                return
            }
        } else {
            var myNodeId = key.publicKeyPoint.getEncoded(false)
            myNodeId = myNodeId.copyOfRange(1, myNodeId.size)
            val helloMessage = HelloMessage(myNodeId, 30303, listOf(capability))
            connection.send(helloMessage)
            helloSent = true
        }
    }

    private fun handle(message: IMessage) {
        println("<<<<<<< $message \n")
        when (message) {
            is HelloMessage -> handle(message)
            is DisconnectMessage -> handle(message)
            is PingMessage -> handle(message)
            is PongMessage -> handle(message)
            else -> listener.onMessageReceived(message)
        }
    }

    private fun handle(message: HelloMessage) {
        helloReceived = true
        proceedHandshake()
    }

    private fun handle(message: DisconnectMessage) {
    }

    private fun handle(message: PingMessage) {
        connection.send(PongMessage())
    }

    private fun handle(message: PongMessage) {
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


    //-----------IPeerConnectionListener methods------------

    override fun connectionKey(): ECKey {
        return key
    }

    override fun onConnectionEstablished() {
        println("DevP2PPeer -> onConnectionEstablished \n")
        proceedHandshake()
    }

    override fun onDisconnected(error: Throwable?) {
        println("DevP2PPeer -> onDisconnected")
    }

    override fun onMessageReceived(message: IMessage) {
        executor.execute {
            handle(message)
        }


    }
}