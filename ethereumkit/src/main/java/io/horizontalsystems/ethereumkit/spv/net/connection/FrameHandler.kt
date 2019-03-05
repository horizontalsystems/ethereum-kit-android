package io.horizontalsystems.ethereumkit.spv.net.connection

import io.horizontalsystems.ethereumkit.spv.net.IMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.Capability
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.DisconnectMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.HelloMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PingMessage
import io.horizontalsystems.ethereumkit.spv.net.devp2p.messages.PongMessage
import kotlin.reflect.KClass

class FrameHandler {
    companion object {
        private val devP2PPacketTypesMap: MutableMap<Int, KClass<out IMessage>> = hashMapOf(
                0x00 to HelloMessage::class,
                0x01 to DisconnectMessage::class,
                0x02 to PingMessage::class,
                0x03 to PongMessage::class)

        private const val devP2PMaxMessageCode = 0x10
    }

    private var frames: MutableList<Frame> = mutableListOf()
    private var packetTypesMap: MutableMap<Int, KClass<out IMessage>> = devP2PPacketTypesMap

    fun register(capabilities: List<Capability>) {
        packetTypesMap = devP2PPacketTypesMap

        var offset = devP2PMaxMessageCode

        capabilities.sortedBy { it.name }.forEach { capability ->
            capability.packetTypesMap.entries.forEach { entry ->
                packetTypesMap[offset + entry.key] = entry.value
            }
            offset = capability.packetTypesMap.keys.max() ?: offset
        }
    }

    fun addFrame(frame: Frame) {
        frames.add(frame)
    }

    fun getMessage(): IMessage? {
        val frame = frames.firstOrNull() ?: return null

        frames.remove(frame)

        val messageClass = packetTypesMap[frame.type]
                ?: throw FrameHandlerError.UnknownMessageType()

        return try {
            messageClass.java.getConstructor(ByteArray::class.java).newInstance(frame.payload)
        } catch (ex: Exception) {
            throw FrameHandlerError.InvalidPayload()
        }
    }

    fun getFrames(message: IMessage): List<Frame> {
        val frames = mutableListOf<Frame>()
        for (entry in packetTypesMap.entries) {
            if (entry.value == message::class) {
                frames.add(Frame(entry.key, message.encoded()))
                break
            }
        }
        return frames
    }

    open class FrameHandlerError : Exception() {
        class UnknownMessageType : FrameHandlerError()
        class InvalidPayload : FrameHandlerError()
    }
}
