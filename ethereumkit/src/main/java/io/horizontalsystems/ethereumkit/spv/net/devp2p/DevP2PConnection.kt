package io.horizontalsystems.ethereumkit.spv.net.devp2p

import io.horizontalsystems.ethereumkit.crypto.ECKey
import io.horizontalsystems.ethereumkit.spv.net.IInMessage
import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.IOutMessage
import io.horizontalsystems.ethereumkit.spv.net.Node
import io.horizontalsystems.ethereumkit.spv.net.connection.FrameConnection
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import java.util.logging.Logger
import kotlin.reflect.KClass

class DevP2PConnection(private val frameConnection: FrameConnection) : FrameConnection.Listener {

    interface Listener {
        fun didConnect()
        fun didDisconnect(error: Throwable?)
        fun didReceive(message: IInMessage)
    }

    private val logger = Logger.getLogger("DevP2PConnection")

    companion object {
        private val devP2PPacketTypesMap: MutableMap<Int, KClass<out IMessage>> = hashMapOf(
                0x00 to HelloMessage::class,
                0x01 to DisconnectMessage::class,
                0x02 to PingMessage::class,
                0x03 to PongMessage::class)

        private const val devP2PMaxMessageCode = 0x10

        fun getInstance(connectionKey: ECKey, node: Node): DevP2PConnection {
            val frameConnection = FrameConnection.getInstance(connectionKey, node)
            val devP2PConnection = DevP2PConnection(frameConnection)

            frameConnection.listener = devP2PConnection

            return devP2PConnection
        }
    }

    var listener: Listener? = null
    private var packetTypesMap: MutableMap<Int, KClass<out IMessage>> = devP2PPacketTypesMap

    @Throws(Exception::class)
    fun register(sharedCapabilities: List<Capability>) {
        packetTypesMap = devP2PPacketTypesMap
        var offset = devP2PMaxMessageCode

        sharedCapabilities.forEach { capability ->
            capability.packetTypesMap.entries.forEach { entry ->
                packetTypesMap[offset + entry.key] = entry.value
            }
            offset = capability.packetTypesMap.keys.maxOrNull() ?: offset
        }
    }

    fun connect() {
        frameConnection.connect()
    }

    fun disconnect(error: Throwable?) {
        frameConnection.disconnect(error)
    }

    fun send(message: IOutMessage) {
        logger.info(">>>>>>>> $message \n")

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
            val message = messageClass.java.getConstructor(ByteArray::class.java).newInstance(payload) as? IInMessage
                    ?: throw UnknownMessageType()
            listener?.didReceive(message)
        } catch (ex: Exception) {
            disconnect(InvalidPayload())
        }
    }

    open class DeserializeError : Exception()
    class UnknownMessageType : DeserializeError()
    class InvalidPayload : DeserializeError()

}
