package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.spv.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.connection.FrameConnection
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import kotlin.reflect.KClass

class DevP2PConnection(private val frameConnection: FrameConnection,
                       val myCapabilities: List<Capability>) : FrameConnection.Listener {

    interface Listener {
        fun didConnect()
        fun didDisconnect(error: Throwable?)
        fun didReceive(message: IMessage)
    }

    companion object {
        private val devP2PPacketTypesMap: MutableMap<Int, KClass<out IMessage>> = hashMapOf(
                0x00 to HelloMessage::class,
                0x01 to DisconnectMessage::class,
                0x02 to PingMessage::class,
                0x03 to PongMessage::class)

        private const val devP2PMaxMessageCode = 0x10

        fun getInstance(myCapabilities: List<Capability>, connectionKey: ECKey, node: Node): DevP2PConnection {
            val frameConnection = FrameConnection.getInstance(connectionKey, node)
            val devP2PConnection = DevP2PConnection(frameConnection, myCapabilities)

            frameConnection.listener = devP2PConnection

            return devP2PConnection
        }
    }

    var listener: Listener? = null
    private var packetTypesMap: MutableMap<Int, KClass<out IMessage>> = devP2PPacketTypesMap

    @Throws(Exception::class)
    fun register(nodeCapabilities: List<Capability>) {

        val sharedCapabilities = mutableListOf<Capability>()

        myCapabilities.forEach { myCapability ->
            if (nodeCapabilities.contains(myCapability))
                sharedCapabilities.add(myCapability)
        }

        check(sharedCapabilities.isNotEmpty()) {
            throw NoCommonCapabilities()
        }

        packetTypesMap = devP2PPacketTypesMap
        var offset = devP2PMaxMessageCode

        sharedCapabilities.sorted().forEach { capability ->
            capability.packetTypesMap.entries.forEach { entry ->
                packetTypesMap[offset + entry.key] = entry.value
            }
            offset = capability.packetTypesMap.keys.max() ?: offset
        }
    }

    fun connect() {
        frameConnection.connect()
    }

    fun disconnect(error: Throwable?) {
        frameConnection.disconnect(error)
    }

    fun send(message: IMessage) {
        println(">>>>>>>> $message \n")
        for (entry in packetTypesMap.entries) {
            if (entry.value == message::class) {
                frameConnection.send(entry.key, message.encoded())
                break
            }
        }
    }

    // ----- FrameConnection.Listener methods ----

    override fun didConnect() {
        listener?.didConnect()
    }

    override fun didDisconnect(error: Throwable?) {
        listener?.didDisconnect(error)
    }

    override fun didReceive(packetType: Int, payload: ByteArray) {
        val messageClass = packetTypesMap[packetType]
                ?: run {
                    disconnect(UnknownMessageType())
                    return
                }

        try {
            val message = messageClass.java.getConstructor(ByteArray::class.java).newInstance(payload)
            listener?.didReceive(message)
        } catch (ex: Exception) {
            disconnect(InvalidPayload())
        }
    }

    open class CapabilityError : Exception()
    class NoCommonCapabilities : CapabilityError()

    open class DeserializeError : Exception()
    class UnknownMessageType : DeserializeError()
    class InvalidPayload : DeserializeError()

}
