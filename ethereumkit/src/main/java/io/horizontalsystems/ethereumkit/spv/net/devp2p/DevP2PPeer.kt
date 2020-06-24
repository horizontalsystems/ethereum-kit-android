package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import java.util.logging.Logger

class DevP2PPeer(val devP2PConnection: DevP2PConnection,
                 val capabilityHelper: CapabilityHelper,
                 val myCapabilities: List<Capability>,
                 val myNodeId: ByteArray,
                 val port: Int) : DevP2PConnection.Listener {

    interface Listener {
        fun didConnect()
        fun didDisconnect(error: Throwable?)
        fun didReceive(message: IInMessage)
    }

    private val logger = Logger.getLogger("DevP2PPeer")

    var listener: Listener? = null

    private fun handle(message: HelloMessage) {
        val sharedCapabilities = capabilityHelper.sharedCapabilities(myCapabilities, message.capabilities)

        check(sharedCapabilities.isNotEmpty()) {
            throw NoSharedCapabilities()
        }

        devP2PConnection.register(sharedCapabilities)

        listener?.didConnect()
    }

    private fun handle(message: DisconnectMessage) {
        devP2PConnection.disconnect(DisconnectMessageReceived())
    }

    private fun handle(message: PingMessage) {
        val pongMessage = PongMessage()
        devP2PConnection.send(pongMessage)
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

    fun send(message: IOutMessage) {
        devP2PConnection.send(message)
    }

    //-----------Connection.Listener methods------------

    override fun didConnect() {
        val helloMessage = HelloMessage(myNodeId, port, myCapabilities)
        devP2PConnection.send(helloMessage)
    }

    override fun didDisconnect(error: Throwable?) {
        listener?.didDisconnect(error)
    }

    override fun didReceive(message: IInMessage) {
        logger.info("<<<<<<< $message \n")
        try {
            when (message) {
                is HelloMessage -> handle(message)
                is DisconnectMessage -> handle(message)
                is PingMessage -> handle(message)
                is PongMessage -> handle(message)
                else -> listener?.didReceive(message)
            }
        } catch (ex: Exception) {
            disconnect(ex)
        }
    }

    open class DevP2PPeerError : Exception()
    class DisconnectMessageReceived : DevP2PPeerError()
    class NoSharedCapabilities : DevP2PPeerError()

    companion object {
        fun getInstance(key: ECKey, node: Node, capabilities: List<Capability>): DevP2PPeer {
            val devP2PConnection = DevP2PConnection.getInstance(key, node)
            var nodeId = key.publicKeyPoint.getEncoded(false)
            nodeId = nodeId.copyOfRange(1, nodeId.size)

            val devP2PPeer = DevP2PPeer(devP2PConnection, CapabilityHelper(), capabilities, nodeId, 30303)

            devP2PConnection.listener = devP2PPeer

            return devP2PPeer
        }
    }
}
